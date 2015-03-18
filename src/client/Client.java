package client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.Server;

public class Client {

    private final String serverAdress;
    private int serverPort;
    private boolean VerrouOK;
    private Socket client;
    private String errorMessage;
    private String user;
    private String password;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Client() {
        this.serverAdress = "127.0.0.1";
        this.serverPort = 3500;
        this.VerrouOK = false;
        this.client = null;
        this.errorMessage = "";
    }

    public Client(String serverAdress, int serverPort, boolean VerrouOK, String user, String password) {
        this.serverAdress = serverAdress;
        this.serverPort = serverPort;
        this.VerrouOK = VerrouOK;
        this.errorMessage = "";
        this.client = null;
        this.user = user;
        this.password = password;
    }
//
//    public static void main(String[] args) {
//        Client client = new Client("127.0.0.1", 3500, false);
//        client.start();
//    }

    public String start() {
        try {
            //Etat Demarrage
            String tad="";
            String request = "";
            StringBuffer message = null;
            System.out.println("Demande de connexion");
            this.client = new Socket();
            this.client.connect(new InetSocketAddress(serverAdress, serverPort));
            DataOutputStream outputStream = new DataOutputStream(this.client.getOutputStream());
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            String content = inputStream.readLine();
            System.out.println("Reponse Serveur :" + content);
            if (content.startsWith("+OK POP3 Server ready tad:")) {
                String[] tabString = content.split("tad:");
                tad = tabString[1].split("\n")[0];
                System.out.println("---------------------"+tad+"-----------------");
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    String mes = tad + "<>"+ password;
                    md.update(mes.getBytes()); 
                    byte byteData[] = md.digest();
                     //convert the byte to hex format method 1
                    message = new StringBuffer();
                    for (int i = 0; i < byteData.length; i++) {
                     message.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
                    }
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
                //Etat Autorisation
                int i = 0;
                while (!VerrouOK && i < 3) {
                    request = "APOP "+user+ " "+message.toString()+"\n";
                    System.out.println("--------------------"+request+"----------------");
                    outputStream.writeBytes(request);
                    String Verrou = inputStream.readLine();
                    System.out.println("Reponse Serveur Verrou :" + Verrou);
                    String[] tabVerrou = Verrou.split(" ");
                    if (tabVerrou[0].equals("+OK")) {
                        //Etat Transaction
                        System.out.println("VerrouOK");
                        this.VerrouOK = true;
                        request = "STAT\n";
                        outputStream.writeBytes(request);
                        String StatResponse = inputStream.readLine();
                        System.out.println("Reponse Serveur Stat :" + StatResponse);
                        String arrResponse[] = StatResponse.split(" ");
                        if (arrResponse[0].equals("+OK") && arrResponse[1] != null) {
                            String MessageReturn = "";
                            for (int j = 1; j < arrResponse.length; j++) {
                                MessageReturn += arrResponse[j] + " ";
                            }
                            return MessageReturn;
                        }
                    } else {
                        i++;
                    }
                }
                if (i >= 3) {
                    //FIN
                    System.out.println("QUIT Probleme de verrou");
                    request = "QUIT\n";
                    outputStream.writeBytes(request);
                    return "Probleme Verrou";
                }

            } else {
                return "Probleme APOP";
            }

        } catch (SocketException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            System.out.println("Err1");
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void deconnexion() {
        String request = "";
        try {
            DataOutputStream outputStream = new DataOutputStream(this.client.getOutputStream());
            request = "QUIT\n";
            outputStream.writeBytes(request);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String Retreive(int numMessage) {
        if (this.VerrouOK && this.client != null) {
            //Etat Transaction
            try {
                String request = "";
                DataOutputStream outputStream = new DataOutputStream(this.client.getOutputStream());
                BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
                ByteArrayOutputStream buffer;
                int essai = 0;
                boolean MessageOK = false;
                while (!MessageOK && essai < 3) {
                    if (numMessage > 0) {
                        request = "RETR " + numMessage + "\n";
                        System.out.println(request);
                        outputStream.writeBytes(request);
                        String MessageResponse = inputStream.readLine();
                        System.out.println(MessageResponse);
                        String arrMessageResponse[] = MessageResponse.split(" ");
                        System.out.println();
                        if (arrMessageResponse[0].equals("+OK")) {
                            MessageOK = true;
                            File tmpFile = new File("Message" + numMessage + ".txt");
                            if (tmpFile.exists()) {
                                tmpFile.delete();
                            }
                            tmpFile.createNewFile();
                            String contenuMessage = "";
                            FileOutputStream fileOutput = new FileOutputStream(tmpFile);
                            for (int i = 1; i < arrMessageResponse.length; i++) {
                                contenuMessage += arrMessageResponse[i] + " ";
                                fileOutput.write(arrMessageResponse[i].getBytes());
                                fileOutput.write(" ".getBytes());
                            }

                            fileOutput.close();
                            return contenuMessage;
                        } else if (arrMessageResponse[0].equals("-ERR")) {
                            MessageOK = true;
                            this.setErrorMessage("Numéro de message invalide");
                        } else {
                            essai++;
                        }
                    } else {
                        essai = 3;
                    }
                }
                if (numMessage == 0) {
                    //FIN
                    System.out.println("Quit Message=0");
                    this.setErrorMessage("Quit Message=0");
                    request = "QUIT\n";
                    outputStream.writeBytes(request);
                }
                if (essai >= 3) {
                    //FIN
                    System.out.println("Quit Message invalide");
                    //this.setErrorMessage("Numéro de message invalide");
                    request = "QUIT\n";
                    //outputStream.writeBytes(request);
                }
            } catch (SocketException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                System.out.println("Err2");
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}
