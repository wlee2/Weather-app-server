package application;

import java.io.*;
import java.util.*;
import java.net.*;
import org.json.JSONObject;


public class MyApp {
	public static void main(String[] args) {
		BuildWeatherServer bws = new BuildWeatherServer();
		bws.start();
	}

}

class BuildWeatherServer extends Thread {
	static ServerSocket mainServer;
	static ArrayList<Socket> sockets;
	static final int weatherPort = 8750;

	BuildWeatherServer() {
		try {
			this.mainServer = new ServerSocket(weatherPort);
			this.sockets = new ArrayList<>();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void run() {
		try {
			Date date = new Date();
			System.out.println("MultiThreadSever started at " + date);		
			while(true) {
				sockets.add(mainServer.accept());
				Connected conn = new Connected(sockets.get(sockets.size() - 1));
				conn.start();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	class Connected extends Thread {		
		Socket socket;
		
		Connected(Socket socket) {
			this.socket = socket;
		}
		
		public void run() {
			try {			
				System.out.println("Connection address=" + this.socket.getInetAddress() + " == login");
        		//ObjectOutputStream os = new ObjectOutputStream(this.socket.getOutputStream());
        		SendThread s = new SendThread();
				s.start();
				s.join();
				System.out.println("Connection from Socket address=" + this.socket.getInetAddress() + " == logout");
				this.socket.close();
				sockets.remove(this.socket);
				System.out.println("Connected = " + sockets.size());
			} catch (Exception e) {
				System.out.println(e);
			}
			
		}
		class SendThread extends Thread {
			public void run() {
				try {
					ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
					while(true) {
						//ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
						//ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
						//String getBeats = null;
						//System.out.println("asd");
						//is = new ObjectInputStream(socket.getInputStream());
						//String getBeats = (String) is.readObject();
						//System.out.println("i did hear a beat from client ");
/*						os.writeObject("1");
						os.flush();*/
						WeatherData wd = new WeatherData();
						wd.getData();
						os.writeObject(wd.getData());
						os.flush();
						Thread.sleep(5 * 1000);
						//os = new ObjectOutputStream(socket.getOutputStream());
					}
				} catch (Exception e) {
					System.out.println(e);
				}
				//os.close();
			}
		}

	}	
}

class WeatherData{
	WData wd;
	String date;

	String condition;
	String description;

	String temp_min;
	String temp_max;
	String temp_normal;
	String err;

	String appID = "e25a93980e827f8da0b0f00890a264a1";

	WeatherData() {
		getWeather();
	}

	void getWeather() {
		try {
			this.err = "fail to connect with Json server";
			String address = "http://api.openweathermap.org/data/2.5/forecast?q=Toronto,ca&mode=json&APPID=" + this.appID;
			URL url = new URL(address);

			//read the json from url
			Scanner scan = new Scanner(url.openStream());
			String results = new String();
			while(scan.hasNext()) {
				results += scan.nextLine();
			}
			scan.close();
			//

			JSONObject obj = new JSONObject(results);

			//System.out.println(obj);

			int cod = obj.getInt("cod");
			if (cod != 200) {
				this.err = "No response from Json server /code = " + cod;
				throw new Exception("fail to connect with json server cod /" + cod );		
			}

			JSONObject listResult = obj.getJSONArray("list").getJSONObject(0);;
			//System.out.println(listResult);
			JSONObject mainResult = listResult.getJSONObject("main");
			//System.out.println(mainResult);
			JSONObject weatherResult = listResult.getJSONArray("weather").getJSONObject(0);
			//System.out.println(weatherResult);

			double tempMin = mainResult.getDouble("temp_min");
			double temp = mainResult.getDouble("temp");
			double tempMax = mainResult.getDouble("temp_max");

			this.date = listResult.getString("dt_txt");

			this.condition = weatherResult.getString("main");
			this.description = weatherResult.getString("description");

			this.temp_min = changeToC(tempMin);
			this.temp_max = changeToC(tempMax);
			this.temp_normal = changeToC(temp);

			this.err = "Updated!";
			wd.Set(condition, description, temp_max, temp_min, temp_normal, date, err);
		} catch (Exception e) {
			System.out.println(e);
			wd.Set(null, null, null, null, null, null, err);
		}
	}

	String changeToC(double b) {
		b = b - 273.15;
		String str = String.format("%.1f", b) + "°C";
		return str;
	}

	WData getData() {
		return this.wd;
	}
}


class WData implements Serializable {
	private static final long serialVersionUID = 1454313033318093811L;
	String condition;
	String description;
	String temp_min;
	String temp_normal;
	String temp_max;
	String date;
	String err;

	WData() {
		this.condition = null;
		this.description = null;
		this.temp_min = null;
		this.temp_normal = null;
		this.temp_max = null;
		this.date = null;
		this.err = null;
	}

	public void Set(String condition, String description, String temp_max, String temp_min, String temp_normal, String date, String err) {
		this.condition = condition;
		this.description = description;
		this.temp_max = temp_max;
		this.temp_min = temp_min;
		this.temp_normal = temp_normal;
		this.date = date;
		this.err = err;
	}

	public void Debug() {
			System.out.println("Date: " + this.date);
			System.out.println("Condition: " + this.condition);
			System.out.println("Description: " + this.description);
			System.out.println("Temp Max/Avg/Min: " + this.temp_max + " / " + this.temp_normal + " / " + this.temp_min);
	}
}