package day06.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务端应用程序
 * 
 * Apache Mina是一个能够帮助用户开发高性能和高伸缩性网络应用程序的框架。它通过Java nio技术基于TCP/IP和UDP/IP协议提供了抽象的、事件驱动的、异步的API。
 * 
 * @author wxw
 */
public class Server {
	/**
	 * 运行在服务端的Socket
	 */
	private ServerSocket server;
	/**
	 * 线程池，用于管理客户端连接的交互线程
	 */
	private ExecutorService threadPool;
	/**
	 * 保存所有客户端输出流的map，多个线程都会操作这个map
	 */
	private Map<String, PrintWriter> allOut;

	/**
	 * 构造方法，用于初始化服务端
	 */
	public Server() throws IOException {
		try {
			System.out.println("初始化服务端");
			// 初始化Socket
			// 创建ServerSocket时需要指定服务端口
			server = new ServerSocket(8088);
			// 初始化线程池
			threadPool = Executors.newFixedThreadPool(10);
			// 初始化存放所有客户端输出流的集合
			allOut = new HashMap<String, PrintWriter>();
			System.out.println("服务端初始化完毕");
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * 服务端开始工作的方法
	 */
	public void start() {
		try {
			while (true) {
				/*
				 * ServerSocket的accept()方法 用于监听8088端口，等待客户端的连接 该方法是一个阻塞方法，直到一个客户端连接， 否则该方法一直阻塞。若一个客户端连接了， 会返回该客户端的Socket
				 */
				System.out.println("等待客户端连接...");
				Socket socket = server.accept();

				/*
				 * 当一个客户端连接后，启动一个线程ClientHandler， 将该客户端的Socket传入，使得该线程处理与客户端的交互。 这样，我们能再次进入循环，接收下一个客户端的连接了。
				 */
				Runnable clientHandler = new ClientHandler(socket);
				// Thread t = new Thread(handler);
				// t.start();
				/*
				 * 使用线程池分配空闲线程来处理当前连接的客户端
				 */
				threadPool.execute(clientHandler);
			}
		} catch (Exception e) {
			System.out.println("服务端启动异常！");
		}
	}

	/**
	 * 将给定的输出流存入共享集合 对"this"(即server对象)上锁
	 * 
	 * 若两个线程在同一段代码看到synchronized锁的是同一个对象，称为"同步锁"
	 * 
	 * 若两个线程在不同的代码看到synchronized锁的是同一个对象，称为"互斥锁"
	 */
	public synchronized void addOut(String nickName, PrintWriter pw) {
		allOut.put(nickName, pw);
	}

	/**
	 * 将给定的输出流从共享集合中删除
	 */
	public synchronized void removeOut(String nickName) {
		allOut.remove(nickName);
	}

	/**
	 * 将给定的消息转发给所有客户端
	 */
	public synchronized void sendMessage(String message) {
		Set<String> keys = allOut.keySet();
		for (String key : keys) {
			PrintWriter pw = allOut.get(key);
			pw.println(message);
		}
	}

	/**
	 * 私聊：将给定的消息转发给给定客户端
	 */
	public synchronized void sendMessage(String fromName, String toName, String message) {
		allOut.get(fromName).println(message);
		allOut.get(toName).println(message);
	}

	/**
	 * 程序入口main
	 */
	public static void main(String[] args) {
		Server server;
		try {
			server = new Server();
			server.start();
		} catch (Exception e) {
			System.out.println("服务端初始化失败");
		}
	}

	/**
	 * 服务端中的一个线程，用于与某个客户端交互 使用线程的目的是使得服务端可以处理多客户端了
	 */
	class ClientHandler implements Runnable {
		/**
		 * 当前线程处理的客户端的socket
		 */
		private Socket socket;
		private InetAddress address;
		/**
		 * 当前线程处理的客户端的IP
		 */
		private String ip;
		/**
		 * 获取远端的端口号
		 */
		private int port;
		/**
		 * 通过刚刚连接上的客户端的Socket获取输入流， 来读取客户端发送过来的信息 将字节输入流包装为字符输入流，这样就可以指定编码集来读取每一个字符 将字符流转换为缓冲字符输入流 这样就可以按行为单位读取字符串了
		 */
		private BufferedReader br;
		/**
		 * 为了让服务端与客户端交互 我们需要通过socket获取输出流 转换为字符流，用于指定编码集 创建缓冲字符输出流，自动行刷新
		 */
		private PrintWriter pw;
		/**
		 * 读取客户端发送过来的一行字符串
		 */
		private String msg;
		/**
		 * 当前客户端的昵称
		 */
		private String nickName;

		/**
		 * 根据给定的客户端的Socket创建线程体
		 */
		public ClientHandler(Socket socket) {
			this.socket = socket;
			/*
			 * 通过socket获取远端的地址信息 对于服务端而言，远端就是客户端了
			 */
			// socket.getLocalAddress();//获取本机的地址信息
			address = socket.getInetAddress();
			// 获取远端计算机的IP地址
			ip = address.getHostAddress();
			// address.getCanonicalHostName();
			// 获取远端的端口号
			port = socket.getPort();
			System.out.println(ip + ":" + port + " 客户端连接了");
		}

		/**
		 * 该线程会将当前Socket中的输入流获取用来循环读取客户端发过来的消息
		 */
		public void run() {
			// 定义在try语句外的目的是：finally中也可以引用到
			try {
				pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"), true);
				br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"));
				// 客户端输入昵称后才能将该客户端的输出流存入共享Map
				// 当创建好当前客户端的输入流后， 读取的第一个字符串应该是昵称
				nickName = br.readLine();
				// 将该客户端的输出流存入共享集合以便使得该客户端也能接收服务端转发的消息
				addOut(nickName, pw);
				// 通知其他用户，该用户上线了
				sendMessage("[" + nickName + "]上线了");
				// 输出当前在线人数
				System.out.println("当前在线人数为：" + allOut.size());
				// System.out.println(allOut.toString()); // 测试共享Map

				/*
				 * 读取客户端发送过来的一行字符串
				 * 
				 * Windows和Linux存在一定的差异： Linux：当客户端与服务端断开连接后，我们通过输入流 会读取到null，但这是合乎逻辑的，因为缓冲流的 readLine()方法若返回null就表示无法通过该流 再读取到信息，参考之前服务文本文件的判断。 Windows：当客户端与服务端断开连接后 readLine()方法会抛出异常。
				 */
				while ((msg = br.readLine()) != null) {
					// 若客户端输入@xxxx:格式将开启私聊模式
					if (msg.matches("^@.+:.+$")) {
						int endIndex = msg.indexOf(":");
						String toName = msg.substring(1, endIndex);
						msg = msg.substring(endIndex + 1);
						sendMessage(nickName, toName, msg);
					} else {
						/*
						 * 增强循环即是利用迭代器，迭代器不是线程安全的，add/remove是同步的，但是遍历与add/remove不是同步的。
						 * 
						 * synchronized修饰"增/删/遍历"这三者之间就即是同步方法，又是互斥关系
						 */
						// for (PrintWriter o : allOut) {
						// o.println(msg);
						// }

						sendMessage(nickName + " 说：" + msg);
					}
				}
			} catch (Exception e) {
				/*
				 * 在Windows中的客户端，报错通常是因为客户端断开连接
				 */
			} finally {
				// 首先将该客户端的输出流从共享集合中删除
				// allOut.remove(pw);
				removeOut(nickName);
				// 通知其他用户，该用户下线了
				sendMessage("[" + nickName + "]下线了");
				// 输出当前在线人数
				System.out.println("当前在线人数为：" + allOut.size());

				/*
				 * 无论是Linux还是Windows用户，当与服务端断开连接后， 我们都应该在服务端与客户端断开连接
				 */
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println("断开连接不成功！");
				}
			}
		}
	}

}
