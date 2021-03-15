import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class myServer extends Thread {
    private static ServerSocket server = null;
    private static int idx = 0;     //user id
    private static int tick = 0;    //who's turn
    private static String action = "";  //an static string for sharing action between clients
    private static String[] user = new String[2];   //user name array
    private static Object action_mutex = new Object();  //for locking action
    private static int all_ready = 0;   //all clients are ready
    private DataInputStream recv = null;
    private DataOutputStream send = null;
    private static int turn = 0;

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setSize(250, 100);
        f.setLayout(new BorderLayout());
        f.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                System.out.println("bye");
                System.exit(0);
            }
        });
        JLabel s = new JLabel("SERVER");
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 60);
        s.setFont(font);
        f.add(s, BorderLayout.CENTER);
        f.setVisible(true);
        
        System.out.println("This is server.");
        try {
            server = new ServerSocket(10000);
        } catch (IOException e) {
            System.out.println("server");
        }
        myServer server1 = new myServer();
        myServer server2 = new myServer();
        server1.start();
        server2.start();
    }

    public void run() {
        int myidx = -1;      //this user's id
        try {
            //accept
            Socket socket = new Socket();
            socket = server.accept();
            System.out.println("client accept.");
            recv = new DataInputStream(new BufferedInputStream(socket.getInputStream())); 
            send = new DataOutputStream(socket.getOutputStream());

            String recv_str = "";
            try {
                //receive user name
                recv_str = recv.readUTF();
                System.out.println("user name : "+recv_str);
                //send id to client
                send.writeUTF(Integer.toString(idx));
                user[idx] = recv_str;
                myidx = idx;                
                System.out.println(idx);
                ++idx;
            } catch (IOException i) {
                System.out.println(i);
            }
            //wait for another player
            try {
                while (idx < 2) sleep(300);
            } catch(InterruptedException e) {
                System.out.println(e);
            }
            //send the other player information
            try {
                //check if waiting room done
                recv_str = recv.readUTF();
                System.out.println(recv_str);
                //send opponent's name
                System.out.println(user[(myidx+1)%2]);
                send.writeUTF(user[(myidx+1)%2]);
            } catch (IOException e) {
                System.out.println(e);
            }
            //both of clients connected and in waiting room
            recv_str = recv.readUTF();
            System.out.println(recv_str);
            try {
                //showing name for 1.5s
                sleep(1500);
            } catch(InterruptedException e) {
                System.out.println(e);
            }
            //tell client can start battle
            send.writeUTF("you can set btn");
            //check if client ready
            recv_str = recv.readUTF();
            System.out.println(recv_str);
            ++all_ready;
            //wait for two client
            while (all_ready != 2) {
                try {
                    sleep(20);
                } catch (InterruptedException ie) {
                    System.out.println(ie);
                }
            }
            System.out.println("all ready");
            //make sure threads are in order
            if (myidx == 0) setPriority(MAX_PRIORITY);
            else {
                setPriority(MIN_PRIORITY);
                try {
                    sleep(20);
                } catch (InterruptedException ie) {
                    System.out.println(ie);
                }
            }
            //interaction
            while (turn < 9) {
                //send tick
                System.out.println("turn = "+turn);
                send.writeUTF(Integer.toString(tick));
                //only one thread at the meantime
                synchronized(action_mutex) {
                    System.out.println(myidx+" : "+Integer.toString(tick));
                    if (tick == myidx) {
                        //receive which btn been clicked
                        action = recv.readUTF();
                        //tell client btn been clicked
                        send.writeUTF("action received");
                        System.out.println("action = "+action);
                        System.out.println(myidx+" = "+Integer.toString(tick));
                        //change priority
                        setPriority(MIN_PRIORITY);
                        //flag that synchronized thread
                        all_ready = -1;
                        //update counter of all player's aciton
                        ++turn;
                    } else {
                        System.out.println(myidx+" != "+Integer.toString(tick));
                        //receive
                        recv_str = recv.readUTF();
                        //send action to the other client
                        send.writeUTF(action);
                        //update turn
                        tick = (tick+1)%2;
                        //flag that synchronized thread
                        all_ready = 1;
                        //change priority
                        setPriority(MAX_PRIORITY);
                    }
                    //recieve status
                    recv_str = recv.readUTF();
                    if (!recv_str.equals("draw")) break;
                    else {
                        if (turn == 9) {
                            send.writeUTF("draw");
                            break;
                        } else {
                            send.writeUTF("continue");
                            recv_str = recv.readUTF();
                        }
                    }
                }
                //if opponent not finish his/her update
                while (all_ready != 1) {
                    try {
                        sleep(20);
                    } catch (InterruptedException ie) {
                        System.out.println(ie);
                    }
                }
            }
            socket.close();
            recv.close();
            send.close();
            server.close();
            
        } catch (IOException i) {
            System.out.println(i);
        }
    }
}