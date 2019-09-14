package de.panda.moduleloader;

import jdk.internal.org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/*
 * Copyright (c) 2019, #PANDA All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ModuleLoader {

    private static List<Class> classes = new ArrayList<>();

    public static void main(String[] args)
    {
        String pathToJar = "ModuleLoader.jar";
        try {
            loadClasses(pathToJar);
        } catch (IOException e) {
            e.printStackTrace();
        }

        findModuleClassAndExecute();
    }

    private static void findModuleClassAndExecute()
    {
        classes.forEach(c -> {
            if(Module.class.isAssignableFrom(c))
            {
                try
                {
                    Object o= c.newInstance();
                    Method m =c.getDeclaredMethod("onLoad", null);
                    m.setAccessible(true);
                    m.invoke(o, null);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }


        }
        });
    }

    private static void loadClasses(String pathToJar) throws IOException {
        URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
        URLClassLoader cl = URLClassLoader.newInstance(urls);
        parseJar(new File(pathToJar)).forEach(c -> {
            try {
                classes.add(cl.loadClass(c));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    public static List<String> parseJar(File targetJar)
            throws IOException {
        List<String> classNames = new ArrayList<>();

        JarFile jarFile = new JarFile(targetJar);

        jarFile.stream()
                .filter(e -> e.getName().endsWith(".class"))
                .forEach(e -> {
                    String className = e.getName().substring(0,e.getName().length()-6);
                    className = className.replace('/', '.');
                    classNames.add(className);
                });

        return classNames;
    }

}
