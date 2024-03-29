package com.my.tools.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ServerSocket {
    private static ServerSocket serverSocket;

    private Selector selector;

    public static void main(String[] args) throws Exception {
        ServerSocket.getInstance().init(8001).listen();
    }

    public static ServerSocket getInstance() {
        if (serverSocket == null) {
            synchronized (ServerSocket.class) {
                if (serverSocket == null) {
                    serverSocket = new ServerSocket();
                }
            }
        }
        return serverSocket;
    }

    public ServerSocket init(int port) throws IOException {
        //初始化channel
        ServerSocketChannel server = ServerSocketChannel.open();

        //绑定本机8001端口
        server.socket().bind(new InetSocketAddress(8001));

        //设置为非阻塞模式
        server.configureBlocking(false);

        //开启selector管理器
        selector = Selector.open();

        //将selector注册至server，并设置只处理accept事件
        server.register(selector, SelectionKey.OP_ACCEPT);

        return this;
    }

    public void listen() throws Exception {
        System.out.println("server start");

        //无限循环持续监听
        while (true) {
            //会阻塞 直到监听到注册的事件
            selector.select();

            //获取唤醒的事件
            Iterator<SelectionKey> selectorKeys = selector.selectedKeys().iterator();

            while (selectorKeys.hasNext()) {
                SelectionKey key = selectorKeys.next();

                //将已取出的SelectionKey删除，防止重复处理
                selectorKeys.remove();

                if (key.isAcceptable()) {

                    //获取到服务端的socket
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

                    //获取接收到的客户端socket
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);

                    //向客户端写消息
                    socketChannel.write(ByteBuffer.wrap(new String("hello, this is server").getBytes()));

                    //注册监听read事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("accept");
                } else if (key.isReadable()) {
                    //使用selector获取channel
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    socketChannel.configureBlocking(false);

                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    //读消息
                    int length = socketChannel.read(buffer);

                    String string = new String(buffer.array(), 0 , length);

                    System.out.println("read:" + socketChannel + string);

                    //写消息
                    socketChannel.write(ByteBuffer.wrap(("server " + System.currentTimeMillis()).getBytes()));
                    Thread.sleep(10000);
                }
            }
        }
    }

}
