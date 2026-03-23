import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * DT聊天小程序-客户端
 * 功能：提供远程线上聊天服务，支持全服公聊以及私聊（不设限），在线用户列表
 * 基于Socket TCP通信 + Swing图形界面 + 多线程处理客户端连接
 * 
 * @author 言川
 * @version 2.0
 */
public class audient extends JFrame {
    private ObjectInputStream my_input;//接收
    private ObjectOutputStream my_output;//输出
    private JTextField my_enter;//输入框
    private JTextArea my_display;//显示框
    private JButton connecButton;//连接按钮
    private JTextField my_name;//用户名称
    private String username;//用户姓名
    private DefaultListModel<String> USERlistModel;
    private Map<String,PrivateChatWindow> IsPrivateWindows = new HashMap<String,PrivateChatWindow>();
    private Map<String,JTextArea> RightWindows = new HashMap<String,JTextArea>();


    
    private class Load extends JFrame{
        private JPanel L_beijing;//背景
        private JTextField L_port;//端口 
        private JTextField L_ip;//服务器地址
        private JButton L_confirm;//确认按钮

        public Load(){
            setTitle("登录");
            setSize(300,280);
            setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));//设置当前窗口排列方式，之后再加入元素就不需要再写排列方式了

            //背景显示
            L_beijing = new JPanel();
            L_beijing.add(new JLabel("欢迎使用DT聊天程序"));
            add(L_beijing);

            //j2作为输入方格，包含了ip输入和端口输入
            JPanel j2 = new JPanel();
            j2.setLayout(new BoxLayout(j2, BoxLayout.Y_AXIS));

            JPanel ipfield = new JPanel();
            ipfield.add(new JLabel("ip: "));
            L_ip = new JTextField(20);
            ipfield.add(L_ip);


            JPanel portfield = new JPanel();
            portfield.add(new JLabel("port: "));
            L_port = new JTextField(20);
            portfield.add(L_port);

            JPanel namefield = new JPanel();
            namefield.add(new JLabel("name: "));
            my_name = new JTextField(20);
            namefield.add(my_name);

            j2.add(ipfield);
            j2.add(portfield);
            j2.add(namefield);
            add(j2);
            
            //确认按钮以及相关逻辑
            L_confirm = new JButton("确认");
            L_confirm.addActionListener(e ->{
                String host = L_ip.getText().trim();
                String portStr = L_port.getText().trim();
                int port;
                try {
                    port = Integer.parseInt(portStr);//数字判断
                } catch (NumberFormatException ex) {
                    L_beijing.add(new JLabel("端口由数字组成"));
                    ex.printStackTrace();
                    return;
                }

                L_confirm.setEnabled(false);
                L_beijing.add(new JLabel("正在连接服务器"+host+":"+port+"..."));

                username = my_name.getText().trim();
                if(username.isEmpty()){
                    username = "用户";
                }

                new Thread(() -> {
                    my_run(host, port,username);
                }).start();

                dispose();
            }); 
            add(L_confirm);

            setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
    }

    //!输出时，如果文字过长，则会导致文本框超出范围，应该整一个类似文本框的东西，宽度和高度随内容和窗口大小改变而改变
    public class PrivateChatWindow extends JFrame{
        private JTextArea display;
        private JTextField Input;

        public PrivateChatWindow(String username){
            super("私人聊天窗口: "+username);

            display = new JTextArea();
            display.setEditable(false);
            RightWindows.put(username, display);
            JScrollPane jsc = new JScrollPane(display);
            Input = new JTextField();
            Input.addActionListener(e->{
                try {
                    String content = Input.getText().trim();
                    my_output.writeObject("PRIVATE:"+username+","+content);
                    my_output.flush();
                    display.append("我: "+content + "\n");
                    Input.setText("");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            JSplitPane js = new JSplitPane(JSplitPane.VERTICAL_SPLIT,jsc,Input);
            js.setDividerLocation(0.8);
            js.setResizeWeight(0.8);
            js.setDividerSize(2);
            add(js,BorderLayout.CENTER);

            setDefaultCloseOperation(HIDE_ON_CLOSE);
            setSize(400,300);
        }
    }

    public audient(){
        super("DT");
        setSize(480,500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        //显示框
        my_display = new JTextArea();
        my_display.setEditable(false);
        //输入框
        my_enter = new JTextField();
        //my_enter.setEditable(false);
        my_enter.addActionListener(e->{
            try {
                String s = my_enter.getText().trim();
                my_output.writeObject(username+": "+s);
                my_output.flush();
                my_displayAppend("我: "+s);
                my_enter.setText("");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        //左侧区域，盛放显示框和输入框
        JSplitPane jleft = new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(my_display),my_enter);
        jleft.setDividerLocation(0.8);
        jleft.setResizeWeight(0.8);//两个搭配才有好的效果
        jleft.setDividerSize(2);

        //  //*自定义分割线
        // jleft.setUI(new BasicSplitPaneUI(){
        //     @Override
        //     public BasicSplitPaneDivider createDefaultDivider(){
        //         return new BasicSplitPaneDivider(this){
        //             @Override
        //             public void paint(Graphics g){
        //                 Graphics2D g2 = (Graphics2D) g;
        //                 g2.setColor(Color.BLUE);
        //                 g2.setStroke(new BasicStroke(2));
        //                 g2.drawLine(0, getHeight()/2, getWidth(), getHeight()/2);
        //             }
        //         };
        //     }
        // });

        //右侧用户栏
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.add(new JLabel("用户列表",SwingConstants.CENTER),BorderLayout.NORTH);

        USERlistModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(USERlistModel);//*后续完成姓名列表的关键
        /**用户列表监听器
         * 鼠标双击打开新窗口  
         */
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e){
                if(e.getClickCount()==2){
                    String selecteduser = userList.getSelectedValue();
                    if(selecteduser!=null&&!IsPrivateWindows.containsKey(selecteduser)){
                        PrivateChatWindow pcw = new PrivateChatWindow(selecteduser);
                        pcw.setLocationRelativeTo(audient.this);
                        pcw.setVisible(true);
                        pcw.toFront();
                        IsPrivateWindows.put(selecteduser, pcw);
                    }else if(IsPrivateWindows.containsKey(selecteduser)){
                        PrivateChatWindow pcw = IsPrivateWindows.get(selecteduser);
                        pcw.setVisible(true);
                        pcw.toFront();
                    }
                }
            }
        });
        userPanel.add(new JScrollPane(userList),BorderLayout.CENTER);

        JScrollPane jright = new JScrollPane(userPanel);

        JSplitPane jsl= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,jleft,jright);
        jsl.setDividerLocation(0.8);
        jsl.setResizeWeight(0.8);
        jsl.setDividerSize(2);

        add(jsl,BorderLayout.CENTER);

        Load LogingWindows = new Load();
        LogingWindows.setAlwaysOnTop(true);
        LogingWindows.setVisible(true);
    }

    public void UPDATA(String[] users){
        SwingUtilities.invokeLater(()->{
            USERlistModel.clear();
            for(String user:users){
                USERlistModel.addElement(user);
            }
        });
    }

    public void AddUserToList(String username){
        SwingUtilities.invokeLater(()->{
            USERlistModel.addElement(username);
        });
    }

    public void RemoveUserFromList(String username){
        SwingUtilities.invokeLater(()->{
            USERlistModel.removeElement(username);
            IsPrivateWindows.remove(username);
        });
    }

    public void my_displayAppend(String s){
        SwingUtilities.invokeLater(()->{
            my_display.append(s+"\n");
            my_display.setCaretPosition(my_display.getText().length());//caret--光标 ，将光标设置在文本的最后
        });
    }

    //判断聊天是否结束
    public boolean my_isEndsession(String m){
        if(m==null) return true;
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
    }//可有可无，判断对话是否结束

    public void my_run(String host,int port,String name){
        try {
                my_displayAppend("尝试连接");

                Socket s = new Socket(host,port);//建立网络联系
                String m ;
                my_displayAppend("连接成功");

                my_output = new ObjectOutputStream(s.getOutputStream());
                my_input = new ObjectInputStream(s.getInputStream());
                SwingUtilities.invokeLater(()->{
                    my_enter.setEnabled(true);
                    my_enter.requestFocusInWindow();
                });
                
                my_output.writeObject("NAME:"+username);//告诉服务器自己的名字

                String list = (String) my_input.readObject();
                String userlist = list.substring(9);
                if(!userlist.isEmpty()){
                    UPDATA(userlist.split(","));
                }
                Boolean b = true;
                while(true){
                    m = (String) my_input.readObject();
                    if(m.startsWith("MSG:")){
                        m = m.substring(4);
                        my_displayAppend(m);
                        if(my_isEndsession(m)){
                            b=false;
                        }
                    }else if(m.startsWith("ONLINE:")){
                        m = m.substring(7);
                        AddUserToList(m);
                    }else if(m.startsWith("OFFLINE:")){
                        m = m.substring(8);
                        RemoveUserFromList(m);
                    }else if(m.startsWith("PRIVATE:")){
                        m = m.substring(8);
                        String[] users = m.split(",");
                        String thisuser=users[0];
                        users[0]="";
                        if(IsPrivateWindows.containsKey(thisuser)==false){
                            PrivateChatWindow pcw = new PrivateChatWindow(thisuser);
                            pcw.setLocationRelativeTo(audient.this);
                            IsPrivateWindows.put(thisuser, pcw);
                        }      
                        IsPrivateWindows.get(thisuser).setVisible(true);
                        IsPrivateWindows.get(thisuser).toFront();
                        JTextArea pjt = RightWindows.get(thisuser);
                        String msg = String.join(",", users);
                        msg = msg.substring(1);
                        pjt.append(thisuser+": "+msg+"\n");
                        //!如何把消息显示到对应的私聊窗口中
                    }
                    if(!b) break; 
                }//多线程并行，使得一边等待，一边发送消息可以实现

                my_output.writeObject("q");
                
                my_output.flush();
                my_output.close();
                my_input.close();
                s.close();
                System.exit(0);
        } catch (Exception e) {
            System.err.println("异常: " + e);
            e.printStackTrace();
            my_displayAppend("连接失败");
            connecButton.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            audient app = new audient();
            app.setVisible(true);
        });
    }
}