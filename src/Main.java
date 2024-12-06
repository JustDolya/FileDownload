import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final Map<String, byte[][]> FILE_SIGNATURES = new HashMap<>();

    static {
        FILE_SIGNATURES.put("mp3", new byte[][]{
                {(byte) 0xFF, (byte) 0xFB}, // MPEG-1 Layer 3
                {(byte) 0x49, (byte) 0x44, (byte) 0x33} // ID3 tag
        });
        FILE_SIGNATURES.put("jpg", new byte[][]{
                {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF} // JPEG
        });
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Выберите тип скачиваемого файла:\n1 - музыка; 2 - фотография");
        int input = scanner.nextInt();
        scanner.nextLine(); // Очистка после nextInt()

        switch (input) {
            case 1:
                handleDownload(scanner, "mp3", "Введите ссылку на скачивание музыки (mp3): ",
                        "Введите имя файла для сохранения музыки: ");
                break;
            case 2:
                handleDownload(scanner, "jpg", "Введите ссылку на скачивание фотографии (jpg): ",
                        "Введите имя файла для сохранения фотографии: ");
                break;
            default:
                System.out.println("Неверный выбор.");
        }
    }

    private static void handleDownload(Scanner scanner, String extension, String urlMessage, String fileNameMessage) {
        System.out.println(urlMessage);
        String url = scanner.nextLine();
        System.out.println(fileNameMessage);
        String fileName = scanner.nextLine();
        if (!fileName.endsWith("." + extension)) {
            fileName += "." + extension; // Добавляем расширение, если его нет
        }
        String finalFileName = fileName;
        new Thread(() -> downloadFile(url, finalFileName)).start();
    }

    private static void openFile(String filePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", filePath);
            pb.start();
        } catch (IOException e) {
            System.out.println("Ошибка при открытии файла: " + e.getMessage());
        }
    }

    public static void downloadFile(String url, String fileName) {
        try {
            URLConnection connection = new URL(url).openConnection();
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                int totalBytes = 0;

                // Считываем и проверяем первые байты (сигнатуру)
                if ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
                    totalBytes += bytesRead;
                    if (!validateFile(buffer, fileName)) {
                        System.out.println("Ошибка: файл " + fileName + " имеет неверный тип.");
                        return;
                    }
                    memoryBuffer.write(buffer, 0, bytesRead);
                }

                // Читаем оставшиеся данные
                while ((bytesRead = input.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    memoryBuffer.write(buffer, 0, bytesRead);
                }

                try (OutputStream output = new FileOutputStream(fileName)) {
                    memoryBuffer.writeTo(output);
                }

                System.out.println("Файл " + fileName + " успешно скачан (" + totalBytes + " байт).");
                openFile(fileName);
            }
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке: " + e.getMessage());
        }
    }

    private static boolean validateFile(byte[] header, String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        byte[][] signatures = FILE_SIGNATURES.get(extension);

        if (signatures == null) {
            System.out.println("Неизвестное расширение файла: " + extension);
            return false;
        }

        for (byte[] signature : signatures) {
            boolean match = true;
            for (int i = 0; i < signature.length; i++) {
                if (header[i] != signature[i]) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }
}
