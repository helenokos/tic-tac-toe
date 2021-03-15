import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.awt.event.*;
import javax.swing.*;

import java.awt.*;

class myClient {
    private static Socket socket = null;
    private static DataOutputStream send = null;
    private static DataInputStream recv = null;
    private String user_name = "";
    private int user_id = -1;
    private static int tick = -1;
    private String recv_str = "";
    private int action = -1;
    private int[] board = new int[] {0,0,0,0,0,0,0,0,0};

    public static void main(String[] args) {
        System.out.println("This is client.");
        myClient client = new myClient();
        client.start_client();
        try {
            socket.close();
            send.close();
            recv.close();
            System.out.println("close");
        } catch (Exception ee) {
            System.out.println(ee);
        }
        System.exit(0);
    }

    public void start_client() {
        try {
            socket = new Socket("127.0.0.1", 10000);
            send = new DataOutputStream(socket.getOutputStream());
            recv = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (Exception e) {
            System.out.println(e);
        }

        //start game
        gameBoard game = new gameBoard();
        gameBoard.Base base = game.new Base();
        gameBoard.BeginScreen begin_screen = game.new BeginScreen();
        base.add(begin_screen);
        base.setVisible(true);

        try {
            //recieve client id from server
            recv_str = recv.readUTF();
            try {
                user_id = Integer.parseInt(recv_str);
            } catch (NumberFormatException nfe) {
                System.out.println(nfe);
            }            
            System.out.println("user id : "+user_id);

            //waiting room
            begin_screen.setVisible(false);
            base.remove(begin_screen);
            gameBoard.WaitingRoom wr = game.new WaitingRoom();
            base.add(wr);
            base.revalidate();
            base.repaint();
            send.writeUTF("waiting room done");

            //recv opponent name
            recv_str = recv.readUTF();
            System.out.println("oppponent name : "+recv_str);
            wr.setVisible(false);
            base.remove(wr);
            gameBoard.getOpponent go = game.new getOpponent(recv_str);
            base.add(go);
            base.revalidate();
            base.repaint();

            //tic tac toe board
            send.writeUTF("print name done");
            //receieve battle if showing name done
            recv_str = recv.readUTF();
            System.out.println(recv_str);
            base.remove(go);
            gameBoard.TicTacToe ttt = game.new TicTacToe();
            base.add(ttt);
            base.revalidate();
            base.repaint();
            if (user_id == 0)
                JOptionPane.showMessageDialog(base, "you are O and first.");
            else
                JOptionPane.showMessageDialog(base, "you are X and second.");
            
            //btn
            gameBoard.btn b = game.new btn();
            base.add(b);
            base.revalidate();
            base.repaint();
            send.writeUTF("btn set");
            System.out.println(recv_str);
            
            //interaction
            while (tick != 100) {
                //recieve tick
                recv_str = recv.readUTF();
                System.out.println(recv_str);
                //convert string to integer
                try {
                    tick = Integer.parseInt(recv_str);
                } catch(NumberFormatException nfe) {
                    System.out.println(nfe);
                }
                //not my turn => update screen
                if (tick != user_id) {
                    //send waiting
                    send.writeUTF("waiting");
                    //receive action
                    recv_str = recv.readUTF();
                    action = Integer.parseInt(recv_str);
                    if (user_id == 0)
                        b.myupdate("X", action);
                    else
                        b.myupdate("O", action);
                } else {
                    //check if btn click
                    recv_str = recv.readUTF();
                }
                base.revalidate();
                base.repaint();
                for (int i = 0; i < 9; ++i) {
                    System.out.print(board[i]);
                }
                System.out.println("");
                if (result() == 0) {
                    send.writeUTF("draw");
                    recv_str = recv.readUTF();
                    if (recv_str.equals("draw")) {
                        JOptionPane.showMessageDialog(base, "draw =|");
                        break;
                    } else {
                        send.writeUTF("continue");
                    }
                } else if (result() == 1) {
                    send.writeUTF("win");
                    JOptionPane.showMessageDialog(base, "you win =)");
                    break;
                } else {
                    send.writeUTF("lose");
                    JOptionPane.showMessageDialog(base, "you lose =(");
                    break;
                }
            }
            base.remove(b);
        } catch (IOException e) {
            System.out.println(e);
        }
        base.setVisible(false);
    }

    class gameBoard {
        //frame
        private class Base extends JFrame {
            private Base() {
                //frame information
                setTitle("tic-tac-toe");
                addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        try {
                            send.writeUTF("Q");
                        } catch (IOException ee) {
                            System.out.println("window exit");
                        }
                        System.out.println("bye");
                        System.exit(0);
                    }
                } );
                setSize(500, 550);
                setLayout(null);
                setResizable(false);
            }
        }
        //begin screen
        private class BeginScreen extends JPanel {
            private BeginScreen() {
                //information
                setSize(500, 550);
                setLayout(null);
    
                //Tic-Tac-Toe
                JLabel tic_tac_toe = new JLabel("Tic-Tac-Toe");
                Font font_ttt = new Font(Font.SANS_SERIF, Font.BOLD, 60);
                tic_tac_toe.setFont(font_ttt);
                tic_tac_toe.setBounds(75, 80, 400, 80);
                add(tic_tac_toe);
    
                //User : 
                JLabel user = new JLabel("User : ");
                Font font_u = new Font(Font.SANS_SERIF, Font.PLAIN, 30);
                user.setFont(font_u);
                user.setBounds(100, 225, 100, 50);
                add(user);
                
                //input name
                JTextField tf = new JTextField();
                tf.setBounds(200, 240, 190, 30);
                add(tf);
    
                //start button
                JButton start = new JButton("START");
                start.setBounds(200, 350, 100, 50);
                start.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("btn");
                        user_name = tf.getText();
                        tic_tac_toe.setVisible(false);
                        user.setVisible(false);
                        tf.setVisible(false);
                        start.setVisible(false);
                        try {
                            //send user name to server
                            send.writeUTF(user_name);  
                        } catch (IOException i) {
                            System.out.println("send user name error");
                            try {
                                socket.close();
                                send.close();
                                recv.close();
                            } catch (Exception ee) {
                                System.out.println(ee);
                            }
                            System.exit(0);
                        }
                        System.out.println("btn out");
                    }
                } );
                add(start);
            }
        }
        //waiting room
        private class WaitingRoom extends JPanel {            
            private WaitingRoom() {
                //information
                setSize(500, 550);
                setLayout(null);

                //vs
                JLabel vs = new JLabel("VS");
                Font font_vs = new Font(Font.SANS_SERIF, Font.BOLD, 50);
                vs.setFont(font_vs);
                vs.setBounds(225,230,70,70);
                add(vs);

                //your name
                JLabel you = new JLabel(user_name);
                Font font_you = new Font(Font.SANS_SERIF, Font.BOLD, 30);
                you.setFont(font_you);
                you.setBounds(150,65,300,70);
                add(you);

                //opponent default waiting
                JLabel opp = new JLabel("waiting..");
                Font font_opp = new Font(Font.SANS_SERIF, Font.BOLD, 30);
                opp.setFont(font_opp);
                opp.setBounds(250,365,300,70);
                add(opp);

                //question mark
                JLabel q = new JLabel("?");
                Font font_q = new Font(Font.SANS_SERIF, Font.BOLD, 30);
                q.setFont(font_q);
                q.setBounds(395,330,70,70);
                q.setForeground(Color.white);
                add(q);
            }
            public void paintComponent(Graphics g) {
                System.out.println("waiting");
                //you
                g.fillOval(50, 30, 70, 70);
                g.fillPolygon(new int[] {50, 85, 120}, new int[] {170, 65, 170}, 3);
                //opponent
                g.fillOval(365, 330, 70, 70);
                g.fillPolygon(new int[] {365, 400, 435}, new int[] {470, 365, 470}, 3);
            }
        }
        //opponent found
        private class getOpponent extends JPanel {
            private getOpponent(String opponent) {
                //information
                setSize(500, 550);
                setLayout(null);

                //vs
                JLabel vs = new JLabel("VS");
                Font font_vs = new Font(Font.SANS_SERIF, Font.BOLD, 50);
                vs.setFont(font_vs);
                vs.setBounds(225,230,70,70);
                add(vs);

                //your name
                JLabel you = new JLabel(user_name);
                Font font_you = new Font(Font.SANS_SERIF, Font.BOLD, 30);
                you.setFont(font_you);
                you.setBounds(150,65,300,70);
                add(you);

                //opponent name
                JLabel opp = new JLabel(opponent);
                Font font_opp = new Font(Font.SANS_SERIF, Font.BOLD, 30);
                opp.setFont(font_opp);
                opp.setBounds(250,365,300,70);
                add(opp);
            }
            public void paintComponent(Graphics g) {
                System.out.println("waiting");
                //you
                g.fillOval(50, 30, 70, 70);
                g.fillPolygon(new int[] {50, 85, 120}, new int[] {170, 65, 170}, 3);
                //opponent
                g.fillOval(365, 330, 70, 70);
                g.fillPolygon(new int[] {365, 400, 435}, new int[] {470, 365, 470}, 3);
            }
        }
        //tic tac toe
        private class TicTacToe extends JPanel {
            private TicTacToe() {
                setSize(500, 550);
                setLayout(null);
            }
            
            public void paintComponent(Graphics g) {
                g.fillRect(200,100,20,300);
                g.fillRect(300,100,20,300);
                g.fillRect(100,200,300,20);
                g.fillRect(100,300,300,20);
            }            
        }
        //circle
        private class circle extends JPanel {
            private circle() {
                setSize(50,50);
                setLayout(null);
            }
            public void paintComponent(Graphics g) {
                g.fillOval(0, 0, getWidth(), getHeight());
            }
        }
        //X
        private class X extends JPanel {
            private X() {
                setSize(50,50);
                setLayout(null);
            }
            public void paintComponent(Graphics g) {
                g.drawLine(0, 0, 50, 50);
                g.drawLine(50, 0, 0, 50);
            }
        }
        //button
        private class btn extends JPanel {
            JButton b1,b2,b3,b4,b5,b6,b7,b8,b9;
            private btn () {
                setSize(500, 500);
                setLayout(null);

                b1 = new JButton("-");
                b1.setBounds(130,140,40,20);
                b1.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[0] = 1;
                        try {
                            //send user action
                            send.writeUTF("1");
                            remove(b1);
                            if (user_id == 0) {
                                circle c1 = new circle();
                                c1.setBounds(125,125,50,50);
                                add(c1);
                            } else {
                                X x1 = new X();
                                x1.setBounds(125,125,50,50);
                                add(x1);
                            }
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b1);
                
                b2 = new JButton("-");
                b2.setBounds(240,140,40,20);
                b2.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[1] = 1;
                        try {
                            //send user action
                            send.writeUTF("2");
                            remove(b2);
                            if (user_id == 0) {
                                circle c2 = new circle();
                                c2.setBounds(235,125,50,50);
                                add(c2);
                            } else {
                                X x2 = new X();
                                x2.setBounds(235,125,50,50);
                                add(x2);
                            }
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b2);

                b3 = new JButton("-");
                b3.setBounds(340,140,40,20);
                b3.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[2] = 1;
                        try {
                            //send user action
                            send.writeUTF("3");
                            remove(b3);
                            if (user_id == 0) {
                                circle c3 = new circle();
                                c3.setBounds(335,125,50,50);
                                add(c3);
                            } else {
                                X x3 = new X();
                                x3.setBounds(335,125,50,50);
                                add(x3);
                            }
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b3);

                b4 = new JButton("-");
                b4.setBounds(130, 250, 40, 20);
                b4.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[3] = 1;
                        try {
                            //send user action
                            send.writeUTF("4");
                            remove(b4);
                            if (user_id == 0) {
                                circle c4 = new circle();
                                c4.setBounds(125,230,50,50);
                                add(c4);
                            } else {
                                X x4 = new X();
                                x4.setBounds(135,230,50,50);
                                add(x4);
                            }
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b4);

                b5 = new JButton("-");
                b5.setBounds(240, 250, 40, 20);
                b5.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[4] = 1;
                        try {
                            //send user action
                            send.writeUTF("5");
                            remove(b5);
                            if (user_id == 0) {
                                circle c5 = new circle();
                                c5.setBounds(235,230,50,50);
                                add(c5);
                            } else {
                                X x5 = new X();
                                x5.setBounds(235,230,50,50);
                                add(x5);
                            }                      
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b5);

                b6 = new JButton("-");
                b6.setBounds(340, 250, 40, 20);
                b6.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[5] = 1;
                        try {
                            //send user action
                            send.writeUTF("6");
                            remove(b6);
                            if (user_id == 0) {
                                circle c6 = new circle();
                                c6.setBounds(335,230,50,50);
                                add(c6);
                            } else {
                                X x6 = new X();
                                x6.setBounds(335,230,50,50);
                                add(x6);
                            }
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b6);

                b7 = new JButton("-");
                b7.setBounds(130, 350, 40, 20);
                b7.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[6] = 1;
                        try {
                            //send user action
                            send.writeUTF("7");
                            remove(b7);
                            if (user_id == 0) {
                                circle c7 = new circle();
                                c7.setBounds(125,330,50,50);
                                add(c7);
                            } else {
                                X x7 = new X();
                                x7.setBounds(125,330,50,50);
                                add(x7);
                            }
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b7);

                b8 = new JButton("-");
                b8.setBounds(240, 350, 40, 20);
                b8.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[7] = 1;
                        try {
                            //send user action
                            send.writeUTF("8");
                            remove(b8);
                            if (user_id == 0) {
                                circle c8 = new circle();
                                c8.setBounds(235,330,50,50);
                                add(c8);
                            } else {
                                X x8 = new X();
                                x8.setBounds(235,330,50,50);
                                add(x8);
                            }
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b8);

                b9 = new JButton("-");
                b9.setBounds(340, 350, 40, 20);
                b9.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tick != user_id) return;
                        board[8] = 1;
                        try {
                            //send user action
                            send.writeUTF("9");
                            remove(b9);
                            if (user_id == 0) {
                                circle c9 = new circle();
                                c9.setBounds(335,330,50,50);
                                add(c9);
                            } else {
                                X x9 = new X();
                                x9.setBounds(335,330,50,50);
                                add(x9);
                            }
                        } catch (IOException i) {
                            System.out.println(i);
                        }
                    }
                });
                add(b9);
            }            
            private void myupdate(String type, int num) {
                board[num-1] = -1;
                if (type.equals("O")) {
                    circle c = new circle();
                    switch (num) {
                    case 1:
                        remove(b1);
                        c.setBounds(125,125,50,50);
                        break;
                    case 2:
                        remove(b2);
                        c.setBounds(235,125,50,50);
                        break;
                    case 3:
                        remove(b3);
                        c.setBounds(335,125,50,50);
                        break;
                    case 4:
                        remove(b4);
                        c.setBounds(125,230,50,50);
                        break;
                    case 5:
                        remove(b5);
                        c.setBounds(235,230,50,50);
                        break;
                    case 6:
                        remove(b6);
                        c.setBounds(335,230,50,50);
                        break;
                    case 7:
                        remove(b7);
                        c.setBounds(125,330,50,50);
                        break;
                    case 8:
                        remove(b8);
                        c.setBounds(235,330,50,50);
                        break;
                    case 9:
                        remove(b9);
                        c.setBounds(335,330,50,50);
                        break;
                    }
                    add(c);
                } else {
                    X x = new X();
                    switch (num) {
                    case 1:
                        remove(b1);
                        x.setBounds(125,125,50,50);
                        break;
                    case 2:
                        remove(b2);
                        x.setBounds(235,125,50,50);
                        break;
                    case 3:
                        remove(b3);
                        x.setBounds(335,125,50,50);
                        break;
                    case 4:
                        remove(b4);
                        x.setBounds(135,230,50,50);
                        break;
                    case 5:
                        remove(b5);
                        x.setBounds(235,230,50,50);
                        break;
                    case 6:
                        remove(b6);
                        x.setBounds(335,230,50,50);
                        break;
                    case 7:
                        remove(b7);
                        x.setBounds(125,330,50,50);
                        break;
                    case 8:
                        remove(b8);
                        x.setBounds(235,330,50,50);
                        break;
                    case 9:
                        remove(b9);
                        x.setBounds(335,330,50,50);
                        break;
                    }
                    add(x);
                }
            }
        }    
    }
    
    // 0 : draw, 1 : win, -1 : lose
    private int result() {
        for (int i = 0; i < 3; ++i) {
            if (board[i*3] == 1 && board[i*3+1] == 1 && board[i*3+2] == 1) return 1;
            else if (board[i] == 1 && board[i+3] == 1 && board[i+6] == 1) return 1;
            else if (board[i*3] == -1 && board[i*3+1] == -1 && board[i*3+2] == -1) return -1;
            else if (board[i] == -1 && board[i+3] == -1 && board[i+6] == -1) return -1;
        }
        if (board[0] == 1 && board[4] == 1 && board[8] == 1) return 1;
        else if (board[0] == -1 && board[4] == -1 && board[8] == -1) return -1;
        else if (board[2] == -1 && board[4] == -1 && board[6] == -1) return -1;
        else if (board[2] == 1 && board[4] == 1 && board[6] == 1) return 1;
        return 0;
    }
}