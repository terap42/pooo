import java.io.*;
import java.net.*;
import java.lang.ProcessBuilder;

public class Serveur {
    private ServerSocket serverSocket;
    private int port;
    private volatile boolean running = false;

    public Serveur(int port) {
        this.port = port;
    }

    public void startServer() {
        if (running) {
            System.out.println("Le serveur est déjà en cours d'exécution.");
            return;
        }
        
        running = true;
        
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serveur démarré sur le port " + port);
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            if (!running) {
                System.out.println("Le serveur a été arrêté.");
            } else {
                e.printStackTrace();
            }
        }
    }

    public void stopServer() {
        if (!running) {
            System.out.println("Le serveur est déjà arrêté.");
            return;
        }
        
        running = false;
        
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);

                String request = in.readLine();
                if (request != null) {
                    System.out.println(request);

                    // Parse la requête HTTP
                    String[] tokens = request.split(" ");
                    String method = tokens[0];
                    String resource = tokens[1];

                    if (method.equals("GET")) {
                        if (resource.equals("/") || resource.equals("/NosDossiers")) {
                            // Si la ressource demandée est la racine ou le répertoire "NosDossiers", envoie le listing des fichiers
                            sendDirectoryListing(out, new File("NosDossiers"));
                        } else {
                            // Sinon, renvoie la ressource demandée
                            sendResource(out, resource);
                        }
                    } else {
                        // Méthode HTTP non supportée
                        sendErrorResponse(out, "405 Method Not Allowed", "Method Not Allowed");
                    }
                }

                out.close();
                in.close();
                clientSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Envoie le contenu d'un fichier au client
        private void sendFile(PrintWriter out, String filename) throws IOException {
            File file = new File(filename);
            if (file.exists() && !file.isDirectory()) {
                out.println("HTTP/1.0 200 OK");
                out.println("Content-Type: " + getContentType(filename)); // Détermine le type de contenu en fonction de l'extension du fichier
                
                // Vérifier si le fichier est un fichier docx
                if (filename.endsWith(".docx")) {
                    out.println("Content-Disposition: inline"); // Ouvrir le fichier dans le navigateur
                }
                
                out.println("Server: Bot");
                out.println("");
        
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                byte[] buffer = new byte[1024];
                int bytesRead;
        
                while ((bytesRead = bis.read(buffer)) != -1) {
                    out.flush(); // Assurez-vous que le tampon est vidé avant d'écrire
                    clientSocket.getOutputStream().write(buffer, 0, bytesRead);
                }
                bis.close();
            } else {
                // Ressource non trouvée
                sendErrorResponse(out, "404 Not Found", "Page Not Found");
            }
        }
        

        // Envoie le contenu d'une ressource au client
        private void sendResource(PrintWriter out, String resource) throws IOException {
            // Décode l'URL pour obtenir le chemin réel
            String decodedResource = URLDecoder.decode(resource, "UTF-8"); // Utilisez directement le nom de l'encodage
            String filePath = "NosDossiers" + decodedResource; // Utilisez le chemin décodé
            System.out.println("Chemin du fichier : " + filePath); // Débogage
            
            File file = new File(filePath);
            if (file.exists()) {
                if (file.isDirectory()) {
                    // Si c'est un répertoire, envoie le listing des fichiers
                    sendDirectoryListing(out, file);
                } else {
                    if (resource.endsWith(".py")) {
                        // Si c'est un fichier Python, exécute le code et envoie le résultat
                        executePythonScript(out, file);
                    } else {
                        // Si c'est un fichier autre que Python, envoie le contenu du fichier
                        sendFile(out, filePath); // Utilisez le chemin du fichier décodé
                    }
                }
            } else {
                // Essayer de rechercher le fichier dans les sous-répertoires de "NosDossiers"
                File[] subDirectories = new File("NosDossiers").listFiles(File::isDirectory);
                if (subDirectories != null) {
                    for (File subDirectory : subDirectories) {
                        File fileInSubDirectory = new File(subDirectory, decodedResource);
                        if (fileInSubDirectory.exists() && !fileInSubDirectory.isDirectory()) {
                            // Si le fichier est trouvé dans un sous-répertoire, envoie le contenu du fichier
                            System.out.println("Chemin du fichier : NosDossiers/" + subDirectory.getName() + decodedResource); // Affiche le chemin HTTP réel avec le sous-répertoire
                            sendFile(out, fileInSubDirectory.getAbsolutePath());
                            return;
                        }
                    }
                }
                // Ressource non trouvée
                sendErrorResponse(out, "404 Not Found", "Page Not Found");
            }
        }
        
        
        // Exécute un script Python et envoie le résultat au client
        private void executePythonScript(PrintWriter out, File file) {
            try {
                // Création d'un processus pour exécuter le script Python
                    ProcessBuilder processBuilder = new ProcessBuilder("python", file.getAbsolutePath());
                    Process process = processBuilder.start();
                 
                // Lecture de la sortie du processus
        
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder pythonOutput = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    pythonOutput.append(line).append("\n"); // Concaténer chaque ligne de sortie
                }

                // Attendre que le processus se termine et récupérer le code de sortie
                int exitCode = process.waitFor();

                // Envoie une réponse HTTP au client
                out.println("HTTP/1.0 200 OK");
                out.println("Content-Type: text/html");
                out.println("Server: Bot");
                out.println("");
                out.println(pythonOutput.toString()); // Envoyer la sortie du script Python
                out.flush(); // Assurez-vous que les données sont envoyées au client
                
            // Vérifier si le script Python s'est terminé avec une erreur
            if (exitCode != 0) {
                // En cas d'erreur, envoie une réponse d'erreur au client
                sendErrorResponse(out, "500 Internal Server Error", "Internal Server Error");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        
        // En cas d'erreur, envoie une réponse d'erreur au client
        sendErrorResponse(out, "500 Internal Server Error", "Internal Server Error");
    }
}

        
        

    // Envoie une page d'erreur au client
    private void sendErrorResponse(PrintWriter out, String status, String message) {
        out.println("HTTP/1.0 " + status);
        out.println("Content-Type: text/html");
        out.println("Server: Bot");
        out.println("");
        out.println("<h1>" + message + "</h1>");
    }

    // Envoie le listing d'un répertoire au client
    private void sendDirectoryListing(PrintWriter out, File directory) {
        out.println("HTTP/1.0 200 OK");
        out.println("Content-Type: text/html");
        out.println("Server: Bot");
        out.println("");

        out.println("<h1>Liste des répertoires</h1>");
        out.println("<ul>");
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String filename = file.getName();
                if (file.isDirectory()) {
                    filename += "/";
                }   
                try {
                    String encodedFilename = URLEncoder.encode(filename, "UTF-8");
                    out.println("<li><a href=\"" + encodedFilename + "\">" + filename + "</a></li>");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
            }   
            }
        }
        out.println("</ul>");
    }

        // Détermine le type de contenu en fonction de l'extension du fichier
        private String getContentType(String filename) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            switch (extension) {
                case "html":
                case "htm":
                    return "text/html";
                case "pdf":
                    return "application/pdf";
                case "doc":
                case "docx":
                    return "application/msword";
                case "txt":
                    return "text/plain";
                case "css":
                    return "text/css";
                case "js":
                    return "application/javascript";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                case "ico":
                    return "image/x-icon";
                case "mp4":
                    return "video/mp4";
                default:
                    return "application/octet-stream"; // Par défaut, utilise le type de contenu générique
            }
        }
    }
}
