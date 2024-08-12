package info.kgeorgiy.ja.olangaev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Implementor implements Impler {

    private static String generatePackageString(Class<?> token) {
        String packageName = token.getPackageName();
        if (packageName.isEmpty()) {
            return "";
        }
        return "package " + token.getPackageName() + ";";
    }

    private static String generateClassBody(Class<?> token) {
        return generateConstructors(token) + System.lineSeparator() + generateMethods(token);
    }

    private static String generateConstructors(Class<?> token) {
        return Arrays.stream(token.getDeclaredConstructors()).filter(constructor -> !Modifier.isPrivate(constructor.getModifiers())).
                map(constructor -> generateSomethingWithScopes(generateConstructorHeader(token, constructor), generateConstructorBody(constructor))
                ).collect(Collectors.joining(System.lineSeparator()));
    }

    private static String generateConstructorBody(Constructor<?> constructor) {
        return "super(" + Arrays.stream(constructor.getParameters()).map(Parameter::getName).collect(Collectors.joining(",")) + ")" + ";";
    }

    private static String generateParametersString(Parameter[] parameters) {
        return "( " + Arrays.stream(parameters).map(parameter -> parameter.getType().getCanonicalName() + " " + parameter.getName()).
                collect(Collectors.joining(",")) + ")";
    }

    private static String generateConstructorHeader(Class<?> token, Constructor<?> constructor) {
        Class<?>[] exceptions = constructor.getExceptionTypes();
        return "public " + getClassSimpleName(token) + generateParametersString(constructor.getParameters())
                + (exceptions.length != 0 ? "throws " + Arrays.stream(exceptions).map(Class::getCanonicalName).collect(Collectors.joining(",")) : "");
    }

    private static String generateMethods(Class<?> token) {
        Set<MethodWrapper> set = new HashSet<>();
        Class<?> current = token;
        while (current != null) {
            set.addAll(Arrays.stream(current.getMethods()).map(MethodWrapper::new).toList());
            set.addAll(Arrays.stream(current.getDeclaredMethods()).map(MethodWrapper::new).toList());
            current = current.getSuperclass();
        }
        return set.stream().filter(method -> Modifier.isAbstract(method.method().getModifiers()) && !Modifier.isNative(method.method().getModifiers())).
                map(method -> generateSomethingWithScopes(generateMethodHeader(method.method()), generateMethodBody(method.method()))).
                collect(Collectors.joining(System.lineSeparator()));
    }

    private static String generateMethodBody(Method method) {
        Class<?> returnType = method.getReturnType();
        String res;
        if (returnType.equals(Void.TYPE)) {
            res = "";
        } else if (returnType.equals(boolean.class)) {
            res = "false";
        } else if (returnType.isPrimitive()) {
            res = "0";
        } else {
            res = "null";
        }
        return "return " + res + ";";
    }

    private static String generateMethodHeader(Method method) {
        return (Modifier.isPublic(method.getModifiers()) ? "public" : "protected") + " " + method.getReturnType().getCanonicalName() + " " + method.getName() + " " +
                generateParametersString(method.getParameters());
    }

    private static String generateSomethingWithScopes(String header, String body) {
        return header + " {" + System.lineSeparator() + body + System.lineSeparator() + "}";
    }

    private static String getClassSimpleName(Class<?> token) {
        return token.getSimpleName().concat("Impl");
    }

    private static String generateClassHeader(Class<?> token) {
        return "public class " + getClassSimpleName(token) + " " + (token.isInterface() ? "implements" : "extends") + " " + token.getCanonicalName();
    }

    private static void checkIsTokenGood(Class<?> token) throws ImplerException {
        if (token.isPrimitive() || token.isArray() || token.isEnum() || token == Enum.class) {
            throw new ImplerException("Can't extend from ".concat(token.getSimpleName()).concat("type"));
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Can't extend final class");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't extend non public class");
        }
        if (!token.isInterface() && Arrays.stream(token.getDeclaredConstructors()).allMatch(constructor -> Modifier.isPrivate(constructor.getModifiers()))) {
            throw new ImplerException("Can't extend there only private constructors");
        }
    }

    private static Path createImplementationPath(Class<?> token, Path root) throws ImplerException {
        Path path = root.resolve(Path.of(token.getPackageName().replace(".", File.separator), getClassSimpleName(token) + ".java"));
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
                return path;
            } catch (IOException e) {
                throw new ImplerException("Can't create directories");
            }
        } else {
            throw new ImplerException("Incorrect root");
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkIsTokenGood(token);
        Path implementationFile = createImplementationPath(token, root);
        try (BufferedWriter writer = Files.newBufferedWriter(implementationFile, StandardCharsets.UTF_8)) {
            writer.write(generatePackageString(token));
            writer.newLine();
            writer.write(generateSomethingWithScopes(generateClassHeader(token), generateClassBody(token)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record MethodWrapper(Method method) {

        private boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2) {
            if (params1.length == params2.length) {
                for (int i = 0; i < params1.length; i++) {
                    if (params1[i] != params2[i])
                        return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MethodWrapper m) {
                if ((this.method.getName().equals(m.method.getName()))) {
                    if (!this.method.getReturnType().equals(m.method.getReturnType()))
                        return false;
                    return equalParamTypes(this.method.getParameterTypes(), m.method.getParameterTypes());
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.method.getName().hashCode() ^ this.method.getReturnType().hashCode() ^ Arrays.hashCode(this.method.getParameterTypes());
        }
    }
}
