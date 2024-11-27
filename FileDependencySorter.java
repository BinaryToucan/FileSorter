import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** класс по поиску зависимостей */
public class FileDependencySorter {

    /** Корневой файл */
    public static String RootDirectory = "Sourse_Success\\";

    /** Файл с итоговым списком */
    public static final String FileForAnswer = "result.txt";

    /** Шаблон для поиска строк вида "require '<путь>'" */
    private static final String REQUIRE_PATTERN = "require\\s+‘(.*?)’";

    /* ------------------------------------------------- изменяемые поля ------------------------------------------------- */

    /** Словарь всех файлов (для отслеживания просмотренных файлов) */
    private Map<String, Boolean> allFiles = new HashMap<>();

    /** Словарь зависимостей  */
    private Map<String, List<String>> dependencies = new HashMap<>();

    /** Множество файлов, которые находятся в текущем стеке */
    private Set<String> onStack = new HashSet<>();

    /** Список файлов */
    private List<String> sortedFiles = new ArrayList<>();

    /* ------------------------------------------------- методы ------------------------------------------------- */

    /** Main метод */
    public static void main(String[] args) throws IOException {
        FileDependencySorter sorter = new FileDependencySorter();
        if(args.length > 0){
            RootDirectory = args[0] + "\\";
        }
        sorter.processDirectory(Paths.get(RootDirectory));
        sorter.sortFiles();
        sorter.writeCombinedFile(FileForAnswer);
    }
    
    /**
     * Метод обход каталога
     * @param root - корневой каталог
     * @throws IOException
     */
    public void processDirectory(Path root) throws IOException {
        Files.walk(root).forEach(path -> {

            // Проверяем что это файл
            if (Files.isRegularFile(path) /*&& path.toString().endsWith(".txt")*/) {
                try {
                    processFile(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Метод обработки файла
     * @param filePath - путь до файла
     * @throws IOException
     */
    public void processFile(Path filePath) throws IOException {

        String fileName = filePath.toString().replace(RootDirectory, ""); // имя файла до корневого файла
        fileName = fileName.substring(0, fileName.lastIndexOf('.')); // и убираем расширение
        fileName = fileName.replace("\\", "/");
        allFiles.put(fileName, false);  // добавляем файл в словарь как непрочитанный
        dependencies.put(fileName, new ArrayList<>());

        // Чтение содержимого файла и поиск директив require с использованием регулярного выражения
        List<String> lines = Files.readAllLines(filePath);
        for (String line : lines) {
            // Применяем шаблон для поиска зависимостей
            Matcher matcher = Pattern.compile(REQUIRE_PATTERN).matcher(line);
            while (matcher.find()) {
                String requiredFile = matcher.group(1);  // Захватываем путь из группы
                dependencies.get(fileName).add(requiredFile);
            }
        }
    }

    /** Проходимся по всем файлам каталога */
    public void sortFiles() {
        for (String file : allFiles.keySet()) {
            if (! allFiles.get(file)) {
                if (!dfs(file)) {
                    return;
                }
            }
        }
    }

    /**
     * Метод по обходу файлов
     * @param file - имя текущего файла
     * @return - отсутсвует ли цикическая зависимость
     */
    private boolean dfs(String file) {

        // Проверяем есть ли вообще такой файл
        if (!allFiles.containsKey(file)) {
            throw new IllegalStateException("Файл не существует: " + file);
        }

        if (onStack.contains(file)) {
            return false; // Обнаружен цикл с файлом
        }
        if (allFiles.get(file)) {
            return true; // Файл уже обработан
        }

        onStack.add(file); // Добавляем файл с стек

        for (String dependency : dependencies.get(file)) {
            if (!dfs(dependency)) {
                throw new IllegalStateException("Циклическая зависимость обнаружена для файлов: '" + file  + "' <-> ' " + dependency + "'");
            }
        }

        onStack.remove(file);
        allFiles.put(file, true); // Помечаем файл как прочитанный
        sortedFiles.add(file);  // Добавляем файл в отсортированный список

        return true;
    }

    /**
     * Записываем результат в файл
     * @param outputFile - имя выходного файла
     * @throws IOException
     */
    public void writeCombinedFile(String outputFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

        for (String file : sortedFiles) {
            writer.write(file);
            writer.newLine();
        }
        writer.close();
        System.out.println("Все файлы успешно склеены в файл: " + outputFile);
    }
}
