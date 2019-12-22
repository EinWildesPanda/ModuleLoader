package de.panda.moduleloader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
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

    private static HashMap<String, Class> classes = new HashMap<>();
    //main : module-class
    private static HashMap<Class, Class> moduleClasses = new HashMap<>();
    private static List<Class> mainClasses = new ArrayList<>();

    private static List<String> mainNames = new ArrayList<>();

    public static void main(String[] args)
    {
        try {
            Files.list(new File("modules/").toPath()).forEach(path ->
            {
                String pathToModule = path.toString();
                try {
                    loadClasses(pathToModule);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        findModuleClassAndExecute();
    }

    private static void findModuleClassAndExecute()
    {
        mainClasses.forEach(clazz -> {

            if(moduleClasses.get(clazz).isAssignableFrom(clazz))
            {
                try
                {
                    Object o = clazz.newInstance();
                    Method m = clazz.getDeclaredMethod("onLoad", null);
                    m.setAccessible(true);
                    m.invoke(o, null);

                    m = clazz.getDeclaredMethod("onDisable", null);
                    m.setAccessible(true);
                    m.invoke(o, null);
                }
                catch(InvocationTargetException | IllegalAccessException | NoSuchMethodException | InstantiationException e)
                {
                    e.printStackTrace();
                }


            }
            else
            {
                System.err.println("Module-class isn't actually a module!");
            }
        });
    }

    private static void loadClasses(String pathToJar) throws IOException {
        //file isn't a jar; just ignore it
        if(!pathToJar.contains(".jar"))
        {
            return;
        }

        URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
        URLClassLoader cl = URLClassLoader.newInstance(urls);

        AtomicReference<String> moduleClass = new AtomicReference<>("");
        AtomicReference<String> mainClazz = new AtomicReference<>("");

        parseJar(new File(pathToJar)).forEach(c -> {
            try {
                if(mainNames.contains(c))
                {
                    //used to know if the main-class was already found and using the name to register the module class
                    mainClazz.set(c);
                    Class mainClass = cl.loadClass(c);

                    mainClasses.add(mainClass);

                    //the module-class was found; so add it to the hashmap with the correct main-class
                    if(!moduleClass.get().isEmpty())
                    {
                        moduleClasses.put(mainClass, cl.loadClass(moduleClass.get()));
                    }
                }
                else
                {
                    //checking if module is the last word in the classname
                    if(c.lastIndexOf("Module") == c.length()-6)
                    {
                        //checking if the main class was found
                        if(!mainClazz.get().isEmpty())
                        {
                            //it was found; so add the module to the hashmap
                            moduleClasses.put(cl.loadClass(mainClazz.get()), cl.loadClass(c));
                        }
                        else
                        {
                            //it wasn't found; so set the name of the module-class
                            moduleClass.set(c);
                        }
                    }

                    //get className
                    classes.put(c, cl.loadClass(c));
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    public static List<String> parseJar(File targetJar)
            throws IOException {
        List<String> classNames = new ArrayList<>();

        JarFile jarFile = new JarFile(targetJar);
        //Will be the path in which the main-class will be available

        jarFile.stream()
                .filter(e -> e.getName().endsWith(".class") || e.getName().endsWith(".yml"))
                .forEach(e -> {
                    //check if the file is the module.yml
                    String mainInfo = "";
                    if(e.getName().equalsIgnoreCase("module.yml"))
                    {
                        try {
                            //Used to read the content of the .yml
                            InputStreamReader inputStreamReader = new InputStreamReader(jarFile.getInputStream(e));
                            BufferedReader reader = new BufferedReader(inputStreamReader);
                            String line;
                            while ((line = reader.readLine()) != null) {
                                //checking if the line is the line we searched
                                if(line.contains("main-class:"))
                                {
                                    //temporarily set the main-class
                                    mainInfo = line.replace("main-class: ", "").replace('/', '.');
                                    mainNames.add(mainInfo);
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    else
                    {
                        String className = e.getName().substring(0,e.getName().length()-6);
                        //check if the class is the main-class
                        className = className.replace('/', '.');
                        classNames.add(className);
                    }

                });

        return classNames;
    }

}
