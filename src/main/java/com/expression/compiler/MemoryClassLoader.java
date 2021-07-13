package com.expression.compiler;

import com.sun.tools.javac.file.BaseFileObject;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.loader.jar.JarFile;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.lang.Nullable;
import sun.misc.PerfCounter;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.*;
import java.lang.reflect.Constructor;
import java.net.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

/**
 * memory class loader
 */
public class MemoryClassLoader extends URLClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(MemoryClassLoader.class);

    private Map<String, byte[]> classBytes = new ConcurrentHashMap<>();

    private static final MemoryClassLoader defaultLoader = new MemoryClassLoader();

    public MemoryClassLoader() {
        super(new URL[0], MemoryClassLoader.class.getClassLoader());
    }

    public MemoryClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * 获取默认的类加载器
     *
     * @return 类加载器对象
     */
    public static MemoryClassLoader getDefaultLoader() {
        return defaultLoader;
    }


    /**
     * 注册Java 字符串到内存类加载器中
     *
     * @param className 类名字
     * @param javaStr   Java字符串
     */
    public void registerJava(String className, String javaStr) {
        try {
            this.classBytes.putAll(compile(className, javaStr));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 自定义Java文件管理器
     *
     * @param var1
     * @param var2
     * @param var3
     * @return
     */
    public static SpringJavaFileManager getStandardFileManager(DiagnosticListener<? super JavaFileObject> var1, Locale var2, Charset var3) {
        Context var4 = new Context();
        var4.put(Locale.class, var2);
        if (var1 != null) {
            var4.put(DiagnosticListener.class, var1);
        }

        PrintWriter var5 = var3 == null ? new PrintWriter(System.err, true) : new PrintWriter(new OutputStreamWriter(System.err, var3), true);
        var4.put(Log.outKey, var5);
        return new SpringJavaFileManager(var4, true, var3);
    }

    /**
     * 编译Java代码
     *
     * @param className 类名字
     * @param javaStr   Java代码
     * @return class 二进制
     */
    public static Map<String, byte[]> compile(String className, String javaStr) {
        //compiler 空指针问题，从jdk/lib下复制tools.jar到 jdk/jre/lib下
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager stdManager = getStandardFileManager(null, null, null);
        try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager);

        ) {
            JavaFileObject javaFileObject = manager.makeStringSource(className, javaStr);
            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, collector, null, null, Arrays.asList(javaFileObject));
            //task 空指针问题，路径中有空格或中文
            if (task.call()) {
                return manager.getClassBytes();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        diagnosticHandler(collector);
        return null;
    }

    private static void diagnosticHandler(DiagnosticCollector<JavaFileObject> collector) {
        //输出编译错误到前端页面
        List<Diagnostic<? extends JavaFileObject>> diagnostics = collector.getDiagnostics();
        Map<Integer, String> errorMsgMap = new HashMap<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            //排除Lombok插件错误信息
            if (!diagnostic.getMessage(Locale.ENGLISH).contains("Lombok")) {
                int lineNum = (int) (diagnostic.getLineNumber() - 8);
                String errorToStr = diagnostic.toString();
                String errorMsgChip = "";
                if (errorMsgMap.containsKey(lineNum)) {
                    errorMsgChip = errorMsgMap.get(lineNum) + "," + diagnostic.getMessage(Locale.ENGLISH);
                } else {
                    errorMsgChip = diagnostic.getMessage(Locale.ENGLISH);
                }
                //去除location信息
                int i = errorMsgChip.indexOf("location");
                errorMsgChip = i > 0 ? errorMsgChip.substring(0, i) : errorMsgChip;
                String arrow = errorToStr.split("\n")[2].replaceAll(" ", "&nbsp;") + "\n";
                String firstVal = errorToStr.split("\n")[1];
                errorMsgMap.put(lineNum, errorMsgChip + "\n" + firstVal + "\n" + arrow);
            }
        }
        String errorMsg = "";
        for (Map.Entry<Integer, String> entry : errorMsgMap.entrySet()) {
            String errorMsgChip = entry.getValue();
            errorMsg = errorMsg + "line:" + entry.getKey() + " ==> \n " + errorMsgChip.replace(",", " ") + "\n";
        }
        if (errorMsg.length() > 0) {
            throw new BizException("999991", "代码编译错误：\n" + errorMsg);
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] buf = classBytes.get(name);
        if (buf == null) {
            return super.findClass(name);
        }
        classBytes.remove(name);
        return defineClass(name, buf, 0, buf.length);
    }


    /**
     * findClass通过类名和表达式进行加载
     * 用于初始化的时候以及集群数据同步
     */
    protected Class<?> findClass(String name, byte[] clazz) {
        return defineClass(name, clazz, 0, clazz.length);
    }

    /**
     * 从文件系统中读取数据 用于添加、修改变量时测试运行
     *
     * @param name 文件名、类名
     * @param path 类所存储的路径
     * @return 返回的Class对象
     */
    protected Class<?> findClass(String name, String path) {
        String classPath = path + name + ".class";
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(classPath);
            outputStream = new ByteArrayOutputStream();
            int temp = 0;
            while ((temp = inputStream.read()) != -1) {
                outputStream.write(temp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte[] bytes = outputStream.toByteArray();
        return defineClass(name, bytes, 0, bytes.length);
    }

    /**
     * 调用默认参数的findClass {@link #findClass(String, byte[])} )}}
     * test时，调用重写的findClass {@link #findClass(String, String)}
     */
    public Class<?> loadClass(String name, @Nullable String path, @Nullable String compilerText) throws IOException {

        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();

                long t1 = System.nanoTime();
                if (path == null && compilerText != null) {
                    //调用本地方法
                    c = findClass(name, Base64.getDecoder().decode(compilerText));
                } else if (path == null) {
                    path = System.getProperty("user.dir");
                    c = findClass(name, path);
                } else {
                    path = path + "/";
                    c = findClass(name, path);
                }

                // this is the defining class loader; record the stats
                PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                PerfCounter.getFindClasses().increment();
            }
            return c;
        }
    }


    /**
     * 开放findClass 给外部使用
     *
     * @param name classname
     * @return class对象
     */
    public Class<?> getClass(String name) throws ClassNotFoundException {
        return this.findClass(name);
    }

    /**
     * 获取jar包所在路径
     *
     * @return jar包所在路径
     */
    public static String getPath() {
        ApplicationHome home = new ApplicationHome(MemoryJavaFileManager.class);
        String path = home.getSource().getPath();
        return path;
    }

    /**
     * 判断是否jar模式运行
     *
     * @return
     */
    public static boolean isJar() {
        return getPath().endsWith(".jar");
    }

}

/**
 * 内存Java文件管理器
 * 用于加载springboot boot info lib 下面的依赖资源
 */
class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    // compiled classes in bytes:
    final Map<String, byte[]> classBytes = new HashMap<>();

    final Map<String, List<JavaFileObject>> classObjectPackageMap = new HashMap<>();

    private JavacFileManager javaFileManager;

    /**
     * key 包名 value javaobj 主要给jdk编译class的时候找依赖class用
     */
    public final static Map<String, List<JavaFileObject>> CLASS_OBJECT_PACKAGE_MAP = new HashMap<>();

    private static final Object lock = new Object();

    private boolean isInit = false;


    /**
     * 添加jar包内所含有的jar包，添加到classpath
     */
    public void init() {
        try {
            String jarBaseFile = MemoryClassLoader.getPath();
            JarFile jarFile = new JarFile(new File(jarBaseFile));
            List<JarEntry> jarEntries = jarFile
                    .stream()
                    .filter(jarEntry -> jarEntry.getName().endsWith(".jar"))
                    .collect(Collectors.toList());

            JarFile libTempJarFile = null;
            List<JavaFileObject> onePackgeJavaFiles = null;
            String packgeName = null;
            for (JarEntry entry : jarEntries) {
                libTempJarFile = jarFile.getNestedJarFile(jarFile.getEntry(entry.getName()));
                if (libTempJarFile.getName().contains("tools.jar")) {
                    continue;
                }
                Enumeration<JarEntry> tempEntriesEnum = libTempJarFile.entries();
                while (tempEntriesEnum.hasMoreElements()) {
                    JarEntry jarEntry = tempEntriesEnum.nextElement();
                    String classPath = jarEntry.getName().replace("/", ".");
                    if (!classPath.endsWith(".class") || jarEntry.getName().lastIndexOf("/") == -1) {
                        continue;
                    } else {
                        packgeName = classPath.substring(0, jarEntry.getName().lastIndexOf("/"));
                        onePackgeJavaFiles = CLASS_OBJECT_PACKAGE_MAP.containsKey(packgeName) ? CLASS_OBJECT_PACKAGE_MAP.get(packgeName) : new ArrayList<>();
                        onePackgeJavaFiles.add(new MemorySpringBootInfoJavaClassObject(jarEntry.getName().replace("/", ".").replace(".class", ""),
                                new URL(libTempJarFile.getUrl(), jarEntry.getName()), javaFileManager));
                        CLASS_OBJECT_PACKAGE_MAP.put(packgeName, onePackgeJavaFiles);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isInit = true;
    }


    MemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
        this.javaFileManager = (JavacFileManager) fileManager;
    }

    public Map<String, byte[]> getClassBytes() {
        return new HashMap<>(this.classBytes);
    }


    @Override
    public void close() throws IOException {
        super.close();
        classBytes.clear();
    }


    public List<JavaFileObject> getLibJarsOptions(String packgeName) {
        synchronized (lock) {
            if (!isInit) {
                init();
            }
        }
        return CLASS_OBJECT_PACKAGE_MAP.get(packgeName);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location,
                                         String packageName,
                                         Set<Kind> kinds,
                                         boolean recurse)
            throws IOException {


        if ("CLASS_PATH".equals(location.getName()) && MemoryClassLoader.isJar()) {
            //如果路径是jar包
            List<JavaFileObject> result = getLibJarsOptions(packageName);// getLibJarsOptions
            if (result != null) {
                return result;
            }
        }

        Iterable<JavaFileObject> it = super.list(location, packageName, kinds, recurse);

        if (kinds.contains(Kind.CLASS)) {
            final List<JavaFileObject> javaFileObjectList = classObjectPackageMap.get(packageName);
            if (javaFileObjectList != null) {
                if (it != null) {
                    for (JavaFileObject javaFileObject : it) {
                        javaFileObjectList.add(javaFileObject);
                    }
                }
                return javaFileObjectList;
            } else {
                return it;
            }
        } else {
            return it;
        }
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof MemoryInputJavaClassObject) {
            return ((MemoryInputJavaClassObject) file).inferBinaryName();
        }
        return super.inferBinaryName(location, file);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind,
                                               FileObject sibling) throws IOException {
        if (kind == Kind.CLASS) {
            return new MemoryOutputJavaClassObject(className);
        } else {
            return super.getJavaFileForOutput(location, className, kind, sibling);
        }
    }

    JavaFileObject makeStringSource(String className, final String code) {
        String classPath = className.replace('.', '/') + Kind.SOURCE.extension;
        return new SimpleJavaFileObject(URI.create("string:///" + classPath), Kind.SOURCE) {
            @Override
            public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
                return CharBuffer.wrap(code);
            }
        };
    }

    void makeBinaryClass(String className, final byte[] bs) {
        JavaFileObject javaFileObject = new MemoryInputJavaClassObject(className, bs);

        String packageName = "";
        int pos = className.lastIndexOf('.');
        if (pos > 0) {
            packageName = className.substring(0, pos);
        }
        List<JavaFileObject> javaFileObjectList = classObjectPackageMap.get(packageName);
        if (javaFileObjectList == null) {
            javaFileObjectList = new LinkedList<>();
            javaFileObjectList.add(javaFileObject);
            classObjectPackageMap.put(packageName, javaFileObjectList);
        } else {
            javaFileObjectList.add(javaFileObject);
        }
    }

    class MemoryInputJavaClassObject extends SimpleJavaFileObject {
        final String className;
        final byte[] bs;

        MemoryInputJavaClassObject(String className, byte[] bs) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
            this.className = className;
            this.bs = bs;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(bs);
        }

        public String inferBinaryName() {
            return className;
        }
    }


    class MemoryOutputJavaClassObject extends SimpleJavaFileObject {
        final String className;

        MemoryOutputJavaClassObject(String className) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
            this.className = className;
        }

        @Override
        public OutputStream openOutputStream() {
            return new FilterOutputStream(new ByteArrayOutputStream()) {
                @Override
                public void close() throws IOException {
                    out.close();
                    ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
                    byte[] bs = bos.toByteArray();
                    classBytes.put(className, bs);
                    makeBinaryClass(className, bs);
                }
            };
        }
    }
}

/**
 * 用来读取springboot的class
 */
class MemorySpringBootInfoJavaClassObject extends BaseFileObject {
    private final String className;
    private URL url;

    MemorySpringBootInfoJavaClassObject(String className, URL url, JavacFileManager javacFileManager) {
        super(javacFileManager);
        this.className = className;
        this.url = url;
    }

    @Override
    public Kind getKind() {
        return Kind.valueOf("CLASS");
    }

    @Override
    public URI toUri() {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getName() {
        return className;
    }

    @Override
    public InputStream openInputStream() {
        try {
            return url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return null;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return null;
    }

    @Override
    public Writer openWriter() throws IOException {
        return null;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public boolean delete() {
        return false;
    }

    public String inferBinaryName() {
        return className;
    }

    @Override
    public String getShortName() {
        return className.substring(className.lastIndexOf("."));
    }

    @Override
    protected String inferBinaryName(Iterable<? extends File> iterable) {
        return className;
    }


    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }


    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return false;
    }
}

/**
 * java 文件管理器 主要用来 重新定义class loader
 */
class SpringJavaFileManager extends JavacFileManager {


    public SpringJavaFileManager(Context context, boolean b, Charset charset) {
        super(context, b, charset);
    }


    @Override
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        nullCheck(location);
        Iterable var2 = this.getLocation(location);
        if (var2 == null) {
            return null;
        } else {
            ListBuffer var3 = new ListBuffer();
            Iterator var4 = var2.iterator();

            while (var4.hasNext()) {
                File var5 = (File) var4.next();

                try {
                    var3.append(var5.toURI().toURL());
                } catch (MalformedURLException var7) {
                    throw new AssertionError(var7);
                }
            }
            return this.getClassLoader((URL[]) var3.toArray(new URL[var3.size()]));
        }
    }

    @Override
    protected ClassLoader getClassLoader(URL[] var1) {
        ClassLoader var2 = this.getClass().getClassLoader();
        try {
            Class loaderClass = Class.forName("org.springframework.boot.loader.LaunchedURLClassLoader");
            Class[] var4 = new Class[]{URL[].class, ClassLoader.class};
            Constructor var5 = loaderClass.getConstructor(var4);
            return (ClassLoader) var5.newInstance(var1, var2);
        } catch (Throwable var6) {
        }
        return new URLClassLoader(var1, var2);
    }


}
