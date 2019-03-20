package application;

import java.io.*;
import java.util.*;
import java.net.*;
import org.json.JSONObject;


public class MyServer {
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
				System.out.println("Connection " + (sockets.indexOf(this.socket) + 1) + " of " + sockets.size() + " closed.");
				sockets.remove(this.socket);
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Connection err");
			}
			
		}
		class SendThread extends Thread {
			ObjectOutputStream os;
			ObjectInputStream is;
			SendThread() {
			}
			public void run() {
				try {
					os = new ObjectOutputStream(socket.getOutputStream());
					is = new ObjectInputStream(socket.getInputStream());
					//String str = (String) is.readObject();
					//System.out.println(str);
					while(true) {
						WeatherData wd = new WeatherData();	
						WDataArray w = new WDataArray();
						w = wd.getData();
						for(int i = 0; i < w.size(); i++)
							w.dataList.get(i).Debug();
						os.writeObject(w);
						Thread.sleep(5 * 60 * 1000);
					}
				} catch (Exception e) {
					System.out.println(e);
					System.out.println("send err");
				}
				//os.close();
			}
		}
	}	
}

class WeatherData{
	WDataArray dataList;

	String appID = "e25a93980e827f8da0b0f00890a264a1";
	String err;

	WeatherData() {
		this.dataList = new WDataArray();
		getWeather();
	}

	void getWeather() {
		try {
			WData tempData;

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
			this.err = "Updated!";
			for(int i = 0; i < 10; i++) {
				JSONObject listResult = obj.getJSONArray("list").getJSONObject(i);
				//System.out.println(listResult);
				JSONObject mainResult = listResult.getJSONObject("main");
				//System.out.println(mainResult);
				JSONObject weatherResult = listResult.getJSONArray("weather").getJSONObject(0);
				//System.out.println(weatherResult);

				double tempMin = mainResult.getDouble("temp_min");
				double temp = mainResult.getDouble("temp");
				double tempMax = mainResult.getDouble("temp_max");

				String date = listResult.getString("dt_txt");

				String condition = weatherResult.getString("main");
				String description = weatherResult.getString("description");

				String temp_min = changeToC(tempMin);
				String temp_max = changeToC(tempMax);
				String temp_normal = changeToC(temp);

				tempData = new WData();
				tempData.Set(condition, description, temp_max, temp_min, temp_normal, date, this.err);
				this.dataList.add(tempData);
			}


		} catch (Exception e) {
			System.out.println(e);
			WData tempData = new WData();
			tempData.Set(null, null, null, null, null, null, this.err);
			this.dataList.add(tempData);
		}
	}

	String changeToC(double b) {
		b = b - 273.15;
		String str = String.format("%.1f", b) + "°C";
		return str;
	}

	WDataArray getData() {
		return this.dataList;
	}
}

class WDataArray implements Serializable {
	private static final long serialVersionUID = 1L;
	public ArrayList<WData> dataList;

	WDataArray() {
		dataList = new ArrayList<>();
	}

	public void add(WData wd) {
		this.dataList.add(wd);
	}

	public void Debug(int i) {
		this.dataList.get(i).Debug();
	}
	public int size() {
		return this.dataList.size();
	}

	public String getCondition(int i) {
		return this.dataList.get(i).getCondition();
	}
	public String getDescription(int i) {
		return this.dataList.get(i).getDescription();
	}
	public String getTemp(int i) {
		return this.dataList.get(i).getTemp();
	}
	public String getTemp_Min(int i) {
		return this.dataList.get(i).getTemp_Min();
	}
	public String getTemp_Max(int i) {
		return this.dataList.get(i).getTemp_Max();
	}
	public String getDate(int i) {
		return this.dataList.get(i).getDate();
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
	
	public String getCondition() {
		return this.condition;
	}
	public String getDescription() {
		return this.description;
	}
	public String getTemp() {
		return this.temp_normal;
	}
	public String getTemp_Min() {
		return this.temp_min;
	}
	public String getTemp_Max() {
		return this.temp_max;
	}
	public String getDate() {
		return this.date;
	}

	public void Debug() {
			System.out.println("Date: " + this.date);
			System.out.println("Condition: " + this.condition);
			System.out.println("Description: " + this.description);
			System.out.println("Temp Max/Avg/Min: " + this.temp_max + " / " + this.temp_normal + " / " + this.temp_min);
	}
}