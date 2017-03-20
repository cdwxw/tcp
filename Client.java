package day06.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * 客户端应用程序
 * 
 * @author wxw
 */
public class Client {
	/**
	 * Socket,用于连接服务端的ServerSocket
	 */
	private Socket socket;
	/**
	 * 为了让服务端与客户端交互 我们需要通过socket获取输出流 转换为字符流，用于指定编码集 创建缓冲字符输出流，自动行刷新
	 */
	private PrintWriter pw;
	/**
	 * 读取客户端发送过来的一行字符串
	 */
	private String msg;
	/**
	 * 创建一个Scanner，用于接收用户输入的字符串
	 */
	private Scanner scan;

	/**
	 * 客户端构造方法，用于初始化客户端
	 */
	public Client() throws Exception {
		try {
			/*
			 * 创建Socket对象时，就会尝试根据给定的地址 与端口连接服务端。 所以，若该对象创建成功，说明与服务端连接正常。
			 */
			System.out.println("正在连接服务端...");
			socket = new Socket("localhost", 8088);
			scan = new Scanner(System.in);
			System.out.println("成功连接服务端");
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * 客户端启动方法
	 */
	public void start() {
		try {
			// 创建并启动线程来接收服务端的消息
			/*
			 * 对于客户端，仅需要2条线程，一条发消息，一条接收服务端消息
			 */
			Runnable serverInfoHandler = new GetServerInfoHandler();
			Thread t = new Thread(serverInfoHandler);
			t.start();

			/*
			 * 可以通过Socket的getOutputStream()方法获取 一条输出流，用于将信息发送至服务端 使用字符流来根据指定的编码集将字符串转换为字节后， 再通过out发送给服务端 将字符流包装为缓冲字符流，就可以按行为单位写出字符串了 自动行刷新
			 */
			pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"), true);

			System.out.println("==========欢迎来到DQ聊天室==========");
			while (true) {
				// 首先输入昵称
				System.out.print("请输入昵称：");
				String nickName = scan.nextLine().trim();
				if (nickName.length() > 0) {
					pw.println(nickName);
					break;
				}
				System.out.println("昵称不能为空");
			}
			while (true) {
				msg = scan.nextLine();
				pw.println(msg);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Client client;
		try {
			client = new Client();
			client.start();
		} catch (Exception e) {
			System.out.println("客户端初始化失败");
		}
	}

	/**
	 * 该线程的作用是循环接收服务端发送过来的信息，并输出到控制台
	 */
	class GetServerInfoHandler implements Runnable {
		/**
		 * 通过刚刚连接上的服务端的Socket获取输入流， 来读取服务端发送过来的信息 将字节输入流包装为字符输入流，这样就可以指定编码集来读取每一个字符 将字符流转换为缓冲字符输入流 这样就可以按行为单位读取字符串了
		 */
		private BufferedReader br;
		/**
		 * 服务端返回的信息
		 */
		String serverMsg;

		public void run() {
			try {
				/*
				 * 通过Socket获取输入流 将输入流转换为字符数如流 将字符输入流转换为缓冲输入流
				 */
				br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"));
				// 循环读取服务端发送的每一行字符串
				while ((serverMsg = br.readLine()) != null) {
					// 将服务端发送的字符串输出到控制台
					System.out.println(serverMsg);
				}
			} catch (IOException e) {
				System.out.println("服务器断开连接..");
			}
		}
	}
}
