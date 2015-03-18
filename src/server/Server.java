package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Server extends Thread {

    private enum Statut {

        AUTHORIZATION, TRANSACTION, CLOSED
    };
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    private Logger logger;
    private FileHandler fh;

    final static int port = 3500;
    private static Socket socket;
    private Statut statut = Statut.AUTHORIZATION;

    private int nb_messages = 0;
    private int volume_messages = 0;
    private String messages[];

    private String timbreADate;

    private Map<String, String> users;

    public String getTimbreADate() {
        return timbreADate;
    }

    public void setTimbreADate(String timbreADate) {
        this.timbreADate = timbreADate;
    }

    public String[] getMessages() {
        return messages;
    }

    public void setMessages(String[] messages) {
        this.messages = messages;
    }

    public int getNb_messages() {
        return nb_messages;
    }

    public void setNb_messages(int nb_messages) {
        this.nb_messages = nb_messages;
    }

    public int getVolume_messages() {
        return volume_messages;
    }

    public void setVolume_messages(int volume_messages) {
        this.volume_messages = volume_messages;
    }

    public Server(Socket socket) {
        this.socket = socket;
        this.logger = Logger.getLogger("MyLog");

        try {
            // This block configure the logger with handler and formatter  
            fh = new FileHandler("U:\\MyLogFile.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // add user
            this.users = new HashMap<String, String>();
            this.users.put("user", "epul");

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //fonction de lecture de fichier 
    public void lectureFichier() {
        String messagerie = "";
        String fichier = "MailBox.txt";
        try {
            FileInputStream fich = new FileInputStream(fichier);
            InputStream ips = fich;
            //on recupere la taille des messages
            setVolume_messages((int) fich.getChannel().size());
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            String ligne;
            while ((ligne = br.readLine()) != null) {
                messagerie = messagerie + ligne;
            }
            setMessages(messagerie.split("\\.<CR><LF>"));
            setNb_messages(getMessages().length);
            br.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            ServerSocket socketServeur = new ServerSocket(port);
            System.out.println("Lancement du serveur");
            while (true) {
                Socket socketClient = socketServeur.accept();
                Server t = new Server(socketClient);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {

            //Connexion avec le client
            String commande = "";
            System.out.println("Connexion avec le client : " + socket.getInetAddress());
            logger.info("Connexion avec le client : " + socket.getInetAddress());

            String clientRequest;
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());

            this.timbreADate = now();
            //Envoi au client le message : OK POP3 Server ready
            outToClient.writeBytes("+OK POP3 Server ready tad:" + this.timbreADate + "\n");
            logger.info("+OK POP3 Server ready" + this.timbreADate + "\n");

            while (statut != Statut.CLOSED) {
                clientRequest = inFromClient.readLine();
                String command[] = clientRequest.split(" ");
                switch (statut) {
                    case AUTHORIZATION:
                        if (command[0].equals("APOP")) {
                            System.out.println("Commande recu :" + command[0]);
                            logger.info("Commande recu :" + command[0]);
                            String login = command[1];
                            String password = command[2];
                            if (users.containsKey(login) && checkUser(login, password)) {
                                //Calcul du nombre de messages et leur taille
                                //String mess = "+OK " + login + lectureFichierNbMessage();
                                //Envoi au client le message : VerrouOK + le nombre de message dans le boite aux lettres
                                lectureFichier();
                                String mess = "+OK " + login + " Vous avez " + getNb_messages() + " message(s) " + getVolume_messages() + " octets" + " \n";
                                System.out.println("Message envoyé :" + mess);
                                logger.info("Message envoyé :" + mess);
                                outToClient.writeBytes(mess);
                                statut = Statut.TRANSACTION;
                            } else {
                                String message_erreur_apop = "-ERR utilisateur invalide\n";
                                System.out.println("Message envoyé :" + message_erreur_apop);
                                logger.severe("Message envoyé :" + message_erreur_apop);
                                outToClient.writeBytes(message_erreur_apop);
                            }
                        } else {
                            String erreur = "Commande non valide\n";
                            System.out.println("Message envoyé :" + erreur);
                            logger.info("Message envoyé :" + erreur);
                            outToClient.writeBytes(erreur);
                        }
                        break;
                    case TRANSACTION:
                        if (command[0].equals("STAT")) {
                            System.out.println("Commande recu :" + command[0]);
                            logger.info("Commande recu :" + command[0]);
                            //lecture du fichier pour recuperer le nombre de messages et leur taille
                            //String message_stat = "+OK" +lectureFichierNbMessage();
                            String message_stat = "+OK Vous avez " + getNb_messages() + " message(s) " + getVolume_messages() + " octets" + " \n";
                            outToClient.writeBytes(message_stat);
                            System.out.println("Message envoyé :" + message_stat);
                            logger.info("Message envoyé :" + message_stat);
                        } else if (command[0].equals("RETR")) {
                            System.out.println("Commande recu :" + command[0] + command[1]);
                            logger.info("Commande recu :" + command[0] + command[1]);
                            int numMessage = Integer.parseInt(command[1]);
                            if (numMessage <= nb_messages) {
                                //String message = lectureMessage(numMessage) + " \n";
                                String message = "+OK " + getMessages()[numMessage - 1] + " \n";
                                outToClient.writeBytes(message);
                                System.out.println("Message envoyé :" + message);
                                logger.info("Message envoyé :" + message);
                            } else {
                                String message_erreur = "-ERR Numero du message invalide. Seulement " + nb_messages + " messages dans le boite.\n";
                                outToClient.writeBytes(message_erreur);
                                System.out.println("Message envoyé :" + message_erreur);
                                logger.severe("Message envoyé :" + message_erreur);
                            }
                            //outToClient.writeBytes(message);
                        } else if (command[0].equals("QUIT")) {
                            System.out.println("Commande recu :" + command[0]);
                            logger.info("Commande recu :" + command[0]);
                            String response = "Au revoir\n";
                            outToClient.writeBytes(response);
                            System.out.println("Message envoyé :" + response);
                            logger.info("Message envoyé :" + response);
                            statut = Statut.CLOSED;
                        }
                        break;
                }
            }
            System.out.println("Fermeture de la session");
            logger.info("Fermeture de la session");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkUser(String login, String cryptMess) {
        String password = users.get(login);
        String original = this.timbreADate + "<>" + password;

        MessageDigest md;
        byte[] digest = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(original.getBytes());
            digest = md.digest();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        //convert the byte to hex format
        StringBuffer hexDigest = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            hexDigest.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
        }
        return hexDigest.toString().equals(cryptMess);
    }

    // Get the current time
    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

}
