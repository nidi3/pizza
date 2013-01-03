package org.mimacom.fun.pizza;

import com.skype.Call;
import com.skype.CallListener;
import com.skype.ContactList;
import com.skype.Friend;
import com.skype.Skype;
import com.skype.SkypeException;
import com.skype.User;
import com.skype.connector.Connector;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: stni
 * Date: 21.06.12
 * Time: 00:52
 * To change this template use File | Settings | File Templates.
 */
public class PizzaFrame {
    public static void main(String[] args) throws IOException, SkypeException {
        try {
            ExecutorService es = Executors.newFixedThreadPool(1);
            es.submit(new Runnable() {
                public void run() {
                    try {
                        Skype.getVersion();
                    } catch (SkypeException e) {
                    }
                }
            });
            es.shutdown();
            if (!es.awaitTermination(5, TimeUnit.SECONDS)) {
                es.shutdownNow();
                JOptionPane.showMessageDialog(null, "Skype seems not to be active");
            } else {
                new PizzaFrame();
            }
        } catch (Throwable e) {
            JOptionPane.showMessageDialog(null, "Problem with Skype: " + e.getMessage());
            System.exit(1);
        }
    }

    public PizzaFrame() throws IOException, SkypeException {
        final Espeak espeak = new Espeak();
        espeak.installIfNeeded();

        JFrame frame = new JFrame("Pizza");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container content = frame.getContentPane();
        BorderLayout layout = new BorderLayout(5, 5);
        content.setLayout(layout);
        frame.setSize(new Dimension(400, 400));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width - frame.getSize().width) / 2, (screenSize.height - frame.getSize().height) / 2);

        final JTextArea text = new JTextArea();
        text.setLineWrap(true);
        text.setBorder(BorderFactory.createEtchedBorder());
        content.add(text, BorderLayout.CENTER);

        final JList contacts = createContactsList();
        content.add(contacts, BorderLayout.EAST);

        JPanel buttons = new JPanel();
        content.add(buttons, BorderLayout.SOUTH);

        final JComboBox lang = createLanguageList();
        buttons.add(lang);

        JButton say = new JButton("Say");
        buttons.add(say);
        say.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                espeak.generate(text.getText().replace("\n", ""), (String) lang.getSelectedItem());
                espeak.say();
            }
        });
        JButton send = new JButton("Send");
        buttons.add(send);
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    espeak.generate(text.getText().replace("\n", ""), (String) lang.getSelectedItem());
                    final File out = espeak.convert();
                    List<String> ids = new ArrayList<String>();
                    for (Object f : contacts.getSelectedValues()) {
                        Friend friend = (Friend) f;
                        ids.add(friend.getId());
                    }
                    final Call call = Skype.call(ids.toArray(new String[ids.size()]));
                    new Thread(new Runnable() {
                        private boolean playing = false;
                        private long started;

                        @Override
                        public void run() {
                            for (; ; ) {
                                try {
                                    Thread.sleep(1000);
                                    if (call.getStatus() != Call.Status.RINGING) {
                                        if (call.getStatus() == Call.Status.INPROGRESS) {
                                            if (playing) {
                                                if (System.currentTimeMillis() > started + espeak.getDuration() + 2000) {
                                                    call.finish();
                                                    break;
                                                }
                                            } else {
                                                playing = true;
                                                Connector c = Connector.getInstance();
                                                String res = c.execute("ALTER CALL " + call.getId() + " SET_INPUT file=\"" + out.getAbsolutePath() + "\"", "ALTER");
                                                started = System.currentTimeMillis();
                                            }
                                        } else {
                                            break;
                                        }
                                    }
                                } catch (Exception e1) {
                                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                }
                            }
                        }
                    }).start();
                } catch (IOException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (SkypeException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        );

        frame.setVisible(true);
    }

    private JList createContactsList() throws SkypeException {
        ContactList cl = Skype.getContactList();
        java.util.List<Friend> friends = new ArrayList<Friend>();
        for (Friend f : cl.getAllFriends()) {
            if (f.getOnlineStatus() == User.Status.ONLINE) {
                friends.add(f);
            }
        }
        Collections.sort(friends, new Comparator<Friend>() {
            @Override
            public int compare(Friend f1, Friend f2) {
                try {
                    return f1.getFullName().compareToIgnoreCase(f2.getFullName());
                } catch (SkypeException e) {
                    return 0;
                }
            }
        });
        JList res = new JList(friends.toArray());
        res.setBorder(BorderFactory.createEtchedBorder());
        res.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                try {
                    setText(((Friend) value).getFullName());
                } catch (SkypeException e) {
                    setText("error");
                }
                return this;
            }
        });
        return res;
    }

    private JComboBox createLanguageList() throws SkypeException {
        JComboBox res = new JComboBox(new Object[]{"en", "de", "es", "fr", "it", "pl", "sk"});
        return res;
    }

}
