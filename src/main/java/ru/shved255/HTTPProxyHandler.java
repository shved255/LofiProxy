package ru.shved255;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class HTTPProxyHandler extends ChannelInboundHandlerAdapter {
	
   private Channel serverChannel;

   public HTTPProxyHandler(final HttpRequest initial, final Channel clientChannel) throws UnknownHostException {
      String host = initial.headers().get("Host").split(":")[0];
      int port = initial.getMethod() == HttpMethod.CONNECT ? Integer.parseInt(initial.headers().get("Host").split(":")[1]) : 80;
      Bootstrap bootstrap = (Bootstrap)((Bootstrap)((Bootstrap)(new Bootstrap()).group(Main.group)).channel(NioSocketChannel.class)).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel channel) {
            channel.pipeline().addLast(new ChannelHandler[]{new ReadTimeoutHandler(30)}).addLast("codec", new HttpClientCodec()).addLast(new ChannelHandler[]{new ChannelInboundHandlerAdapter() {
               public void channelActive(ChannelHandlerContext ctx) {
                  HTTPProxyHandler.this.serverChannel = ctx.channel();
                  if(initial.getMethod() == HttpMethod.CONNECT) {
                     clientChannel.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)).addListener((future) -> {
                        HTTPProxyHandler.this.serverChannel.pipeline().remove("codec");
                        clientChannel.pipeline().remove("codec");
                        clientChannel.config().setAutoRead(true);
                     });
                  } else {
                	  HTTPProxyHandler.this.serverChannel.writeAndFlush(initial).addListener((future) -> {
                        HTTPProxyHandler.this.serverChannel.pipeline().remove("codec");
                        clientChannel.pipeline().remove("codec");
                        clientChannel.config().setAutoRead(true);
                     });
                  }

               }

               public void channelRead(ChannelHandlerContext ctx, Object msg) {
                  clientChannel.writeAndFlush(msg);
               }

               public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                  ctx.close();
               }
            }});
         }
      });
      bootstrap.connect(InetAddress.getByName(host).getHostAddress(), port).channel().closeFuture().addListener((future) -> {
         clientChannel.close();
      });
   }
   
   public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if(this.serverChannel != null) {
         this.serverChannel.writeAndFlush(msg);
      }

   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      ctx.close();
   }

   public void channelInactive(ChannelHandlerContext ctx) {
      if(this.serverChannel != null) {
         this.serverChannel.close();
      }

   }
}

