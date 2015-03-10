package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.regex.Pattern;

public class Server extends Thread {

    private enum Statut {

        AUTHORIZATION, TRANSACTION, CLOSED
    };
    final static int port = 3500;
    private static Socket socket;
    private Statut statut = Statut.AUTHORIZATION;

    private int nb_messages = 0;
    private int volume_messages = 0;
    private String messages[];

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

    //fonction de lecture de fichier pour recuperer le nombre des messages et leur tailles
    /*public String lectureFichierNbMessage(){
     int nbMessages = 0;
     String message = "";
     String fichier ="messagerie.txt";
     try {
     FileInputStream fich = new FileInputStream(fichier);
     InputStream ips = fich;
     //on recupere la taille des messages
     setVolume_messages((int)fich.getChannel().size());
     InputStreamReader ipsr = new InputStreamReader(ips);
     BufferedReader br = new BufferedReader(ipsr);
     String ligne;
     while ((ligne = br.readLine()) != null) {
     if (ligne.equals(".<CR><LF>")){
     nbMessages++;
     }
     }
     br.close();
     setNb_messages(nbMessages);
     message = " Vous avez " + getNb_messages() + " message(s) "+ getVolume_messages() +  "octets" + "\n";
     return message;
     } catch (Exception e) {
     System.out.println(e.toString());
     }
     return null;
     } 
    
     //fonction pour lire le fichier et recupere le message demandé
     public String lectureMessage(int numMessage){
     String messagerie = "";
     String message = "";
     String fichier ="messagerie.txt";
     try {
     FileInputStream fich = new FileInputStream(fichier);
     InputStream ips = fich;
     //on recupere la taille des messages
     setVolume_messages((int)fich.getChannel().size());
     InputStreamReader ipsr = new InputStreamReader(ips);
     BufferedReader br = new BufferedReader(ipsr);
     String ligne;
     while ((ligne = br.readLine()) != null) {
     messagerie = messagerie + ligne;
     }
     String messages[] = messagerie.split("\\.<CR><LF>");
     message = messages[numMessage-1];
     br.close();
     return message;
     } catch (Exception e) {
     System.out.println(e.toString());
     }
     return null;
     }*/
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

            String clientRequest;
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());

            //Envoi au client le message : OK POP3 Server ready
            outToClient.writeBytes("+OK POP3 Server ready\n");

            while (statut != Statut.CLOSED) {
                clientRequest = inFromClient.readLine();
                String command[] = clientRequest.split(" ");
                switch (statut) {
                    case AUTHORIZATION:
                        if (command[0].equals("APOP")) {
                            System.out.println("Commande recu :" + command[0]);
                            String login = command[1];
                            String password = command[2];
                            if (login.equals("user") && password.equals("epul")) {
                                //Calcul du nombre de messages et leur taille
                                //String mess = "+OK " + login + lectureFichierNbMessage();
                                //Envoi au client le message : VerrouOK + le nombre de message dans le boite aux lettres
                                lectureFichier();
                                String mess = "+OK " + login + " Vous avez " + getNb_messages() + " message(s) " + getVolume_messages() + " octets" + " \n";
                                System.out.println("Message envoyé :" + mess);
                                outToClient.writeBytes(mess);
                                statut = Statut.TRANSACTION;
                            } else {
                                String message_erreur_apop = "-ERR utilisateur invalide\n";
                                System.out.println("Message envoyé :" + message_erreur_apop);
                                outToClient.writeBytes(message_erreur_apop);
                            }
                        } else {
                            String erreur = "Commande non valide\n";
                            System.out.println("Message envoyé :" + erreur);
                            outToClient.writeBytes(erreur);
                        }
                        break;
                    case TRANSACTION:
                        if (command[0].equals("STAT")) {
                            System.out.println("Commande recu :" + command[0]);
                            //lecture du fichier pour recuperer le nombre de messages et leur taille
                            //String message_stat = "+OK" +lectureFichierNbMessage();
                            String message_stat = "+OK Vous avez " + getNb_messages() + " message(s) " + getVolume_messages() + " octets" + " \n";
                            outToClient.writeBytes(message_stat);
                            System.out.println("Message envoyé :" + message_stat);
                        } else if (command[0].equals("RETR")) {
                            System.out.println("Commande recu :" + command[0] + command[1]);
                            int numMessage = Integer.parseInt(command[1]);
                            if (numMessage <= nb_messages) {
                                //String message = lectureMessage(numMessage) + " \n";
                                String message = "+OK " + getMessages()[numMessage - 1] + " \n";
                                outToClient.writeBytes(message);
                                System.out.println("Message envoyé :" + message);
                            } else {
                                String message_erreur = "-ERR Numero du message invalide. Seulement " + nb_messages + " messages dans le boite.\n";
                                outToClient.writeBytes(message_erreur);
                                System.out.println("Message envoyé :" + message_erreur);
                            }
                            //outToClient.writeBytes(message);
                        } else if (command[0].equals("QUIT")) {
                            System.out.println("Commande recu :" + command[0]);
                            String response = "Au revoir\n";
                            outToClient.writeBytes(response);
                            System.out.println("Message envoyé :" + response);
                            statut = Statut.CLOSED;
                        }
                        break;
                }
            }
            System.out.println("Fermeture de la session");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
