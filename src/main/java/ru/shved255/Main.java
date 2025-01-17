package ru.shved255;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Scanner;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class Main extends Thread {
	public static final EventLoopGroup group = (EventLoopGroup)new NioEventLoopGroup();
	public static LinkedHashSet<Integer> adrs = new LinkedHashSet<Integer>();
	public static int ports;
	public static int a;
	public static int b;
	public static String ip;
  
	Main(String name){
		super(name);
	}
  
	private static String getExternalIp() {
		try {
			URL url = new URL("https://whatismyip.akamai.com/");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET");
	        connection.setConnectTimeout(5000); 
	        connection.setReadTimeout(5000); 
	        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()))) {
	            return reader.readLine();
	        }
	    } catch (Exception e) {
	        return "Не удалось получить внешний IP: " + e.getMessage();
	    }
	}
  
	public static void main(String[] args) throws UnknownHostException {
		String[] port;
	    int portStart;
	    int portEnd;
	    System.out.println("HTTP PROXY servers by shved255\n");
	    try (Scanner scanner = new Scanner(System.in)) {
		    System.out.print("[/] Введите диапозон портов прокси (10000-10002): ");
	      port = scanner.nextLine().split("-");
	      portStart = Integer.parseInt(port[0]);
	      portEnd = Integer.parseInt(port[1]);
	      System.out.println("[/] Введите айпи на котором будет прокси, введите 0 если хотите чтобы определяло автоматически (0): ");
	      String ip2 = scanner.nextLine();
	      if(ip2.equalsIgnoreCase("0")) {
	    	  ip = getExternalIp();
	      } else {
	    	  ip = ip2;
	      }
	      if(portStart > portEnd) {
	    	  System.out.println("Введите правильный диапозон!");
	    	  return;
	      }
	      a = portEnd - portStart;
	      System.out.println("Будет создано: "+ a + " прокси.");
	    } 
	    b = 0;
	    while(true) {
	    	ports = Choice.getRandomInt(portStart, portEnd);
	    	if(adrs.contains(ports)) {
    	
	    	} else {	
	    		try {
	    			Thread.sleep(500);
	    		} catch (InterruptedException e) {
	    			e.printStackTrace();
	    		}
	    		if(b>a) {
	    			System.out.println("Программа завершила работу успешно");
	    			return;	
	    		}
	    		if(b<a) {
	    			b++;
	    			adrs.add(ports);
	    			((ServerBootstrap)(new ServerBootstrap())
	    					.group((EventLoopGroup)new NioEventLoopGroup(1))
	    					.channel(NioServerSocketChannel.class))
	    					.childHandler((ChannelHandler)new ChannelInitializer<SocketChannel>() {
	    				protected void initChannel(SocketChannel channel) {
	    					channel.pipeline()
	    					.addLast(new ChannelHandler[] { (ChannelHandler)new ReadTimeoutHandler(20) }).addLast("codec", (ChannelHandler)new HttpServerCodec())
	    					.addLast("handler", (ChannelHandler)new ChannelInboundHandlerAdapter() {
	    						public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnknownHostException {
	    							if(msg instanceof io.netty.handler.codec.http.DefaultHttpRequest) {
	    								ctx.channel().config().setAutoRead(false);
	    								ctx.channel().pipeline().replace("handler", "handler", (ChannelHandler)new HTTPProxyHandler((HttpRequest)msg, ctx.channel()));
	    								;
	    							} 
	    						}
                  
	    						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	    							ctx.close();
	    						}
	    					});
	    				}  
	    			}).bind("0.0.0.0", ports).addListener(future -> {
	    				if(!adrs.contains(ports)) {
	    					if(future.isSuccess()) {
	    						System.out.println("[+] Удачно был создан прокси на порту " + ports + " ("+ ip + ":" + ports + ")");
	    						try (BufferedWriter writer1 = new BufferedWriter(new FileWriter("Proxies.txt", true))) {
	    							writer1.write(ip + ":" + ports + "\n");
	    						}
	    					} else {
	    						System.out.println("[-] Не получилось создать прокси на порту " + ports);
	    					}
	    					
          					} else {
          					
          					}
        				});
    				}
    			}
    		}
  		}
	}
