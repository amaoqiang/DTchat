import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * DT聊天小程序 - 服务器端主程序
 * 功能:提供多客户端聊天服务，支持全服群聊、私聊、用户上下线通知、在线用户列表
 * 基于Socket TCP通信 + Swing图形界面 + 多线程处理客户端连接
 * 
 * @author 言川
 * @version 2.0
 */
public class server extends JFrame {
    //输入信息文本框
    private JTextField my_enter;
    //消息展示文本域
    private JTextArea my_display;
    //private int my_clientNumber = 0;
    //在线客户端集合：key=用户名，value=对应输出流，
    private Map<String,ObjectOutputStream>client = new HashMap<>();
    
    /**
     * 构造方法：初始化服务器图形界面
     * 包含消息输入框、消息展示区域、事件监听
     */
    public server(){
        super("聊天程序服务器端");
        Container c = getContentPane();
        //初始化消息输入框，默认禁用，等待服务器启动后启用
        my_enter = new JTextField();
        my_enter.setEnabled(false);
        //输入框回车事件，向所有在线客户端发送服务器公告
        my_enter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event){
                try {
                    //获取输入的公告内容
                    String s = event.getActionCommand();
                    //遍历所有客户端，广播服务器公告
                    for(ObjectOutputStream out : client.values()){
                        out.writeObject("MSG:服务器公告: "+s);
                        out.flush();
                    }
                    //服务器界面展示公告
                    my_displayAppend("服务器公告: "+ s);
                    //清空输入框
                    my_enter.setText("");
                } catch (Exception e) {
                    System.err.println("异常");
                    e.printStackTrace();
                }
            }
        });
        c.add(my_enter, BorderLayout.NORTH);

        //初始化消息展示区域，设置为不可编辑
        my_display = new JTextArea();
        my_display.setEditable(false);
        c.add(new JScrollPane(my_display), BorderLayout.CENTER);
    }

    /**
     * 安全追加消息到界面展示区
     * 使用SwingUtilities保证UI操作在主线程执行，避免线程安全问题
     * 
     * @param s 要展示的消息内容
     */
    public void my_displayAppend(String s){
        SwingUtilities.invokeLater(()->{
            my_display.append(s+"\n");
            my_display.setCaretPosition(my_display.getText().length());
        });
    }

    /**
     * 判断客户端是否发出退出指令
     * 
     * @param m 客户端发出的消息
     * @return true=退出指令 false=正常消息
     */
    public boolean my_isEndsession(String m){

        switch (m) {
            case "q":
            case "quit":
            case "exit":
            case "end":
            case "结束":
                return true;
            default:
                return false;
        }
    }

    /**
     * 服务器核心运行方法
     * 1.启动ServerSocket监听端口5000
     * 2.循环接受客户端连接
     * 3.为每个客户端创建独立线程处理通信
     */
    public void my_run(){
        try {
            // 启动服务器，监听5000端口
            ServerSocket server = new ServerSocket(5000);
            // 服务器启动成功，启用输入框
            SwingUtilities.invokeLater(()->{my_enter.setEnabled(true);});
            
            //循环等待用户端连接
            while(true){
                my_displayAppend("等待客户端连接");
                //阻塞等待客户端连接
                Socket s = server.accept();
                my_displayAppend("新客户端连接成功");

                //为每个客户端创建独立线程，处理通信逻辑
                new Thread(()->{
                    String username = null;
                    try {
                        // 获取客户端输入输出流
                        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

                        //读取客户端发送的用户名（格式：USER:xxx）
                        String message = (String)in.readObject();
                        username = message.substring(5);//截取用户的昵称
                        my_displayAppend("用户【"+username+"】上线");

                        //广播给所有的用户端有新用户上线，便于其余用户端更新用户列表
                        for(ObjectOutputStream output: client.values()){
                            output.writeObject("ONLINE:"+username);
                            output.flush();
                        }

                        //广播整个用户列表，有更好的方法
                        // for(String c : client.keySet()){
                        //     out.writeObject("USERLIST:"+c);
                        // }

                        //向新连接的客户端发送当前在线用户列表
                        if(!client.isEmpty()){
                            String userlist ="USERLIST:"+String.join(",", client.keySet());
                            out.writeObject(userlist);
                            out.flush();
                        }else{
                            out.writeObject("USERLIST:");
                            out.flush();
                        }

                        // 将当前用户加入在线集合
                        client.put(username, out);

                        //循环处理客户端消息
                        while(true){
                            String m =(String) in.readObject();

                            // 私聊信息处理：格式 PRIVATE:目标用户，消息内容
                            if(m.startsWith("PRIVATE:")){
                                m = m.substring(8);
                                String[] users = m.split(",");
                                //截取目标用户名
                                ObjectOutputStream pout = client.get(users[0]);
                                //重组消息并发送
                                users[0]="";
                                pout.writeObject("PRIVATE:"+username+String.join(",", users));
                                pout.flush();
                            }else{
                                //群聊消息处理
                                my_displayAppend(m);
                                for(ObjectOutputStream ob : client.values()){
                                    //广播给除发送者外的所有客户端
                                    if(ob.equals(out))
                                        continue;
                                    ob.writeObject("MSG:"+m);
                                    ob.flush();
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 客户端异常断开处理
                        e.printStackTrace();
                        if(username != null){
                            //从在线集合移除用户
                            client.remove(username);
                            my_displayAppend("用户"+username+"下线");
                            //广播下线通知给所有在线用户
                            for(ObjectOutputStream ob : client.values()){
                                try {
                                    ob.writeObject("OFFLINE:"+username);
                                    ob.flush();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println(" 服务器启动异常");
            e.printStackTrace();
        }
    }

    /**
     * 程序入口方法
     * 初始化服务器界面并启动服务
     * @param args
     */
    public static void main(String[] args) {
        server app = new server();

        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setSize(350,150);
        app.setVisible(true);
        app.my_run();
    }
}
