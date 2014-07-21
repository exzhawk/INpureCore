package info.inpureprojects.core.Scripting;

import com.google.common.eventbus.EventBus;
import info.inpureprojects.core.API.Events.*;
import info.inpureprojects.core.Scripting.Objects.ExposedObject;
import info.inpureprojects.core.Utils.Timer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by den on 7/16/2014.
 */
public class ScriptingCore {

    public EventBus bus = new EventBus();
    public EventBus forwardingBus = new EventBus();
    private HashMap<String, ScriptEngine> engines = new HashMap();
    private ArrayList<ExposedObject> exposedObjects = new ArrayList();
    private File scriptFolder;
    private File scriptCache;
    private File saveFolder;

    public ScriptingCore() {
    }

    public File getScriptFolder() {
        return scriptFolder;
    }

    public void doReload() {
        Timer t = new Timer();
        t.start();
        System.out.println("Starting script reload process...");
        this.doSave();
        forwardingBus.post(new EventReloadScripts());
        this.clearForwardBus();
        this.engines.clear();
        this.doSetup();
        this.loadScripts();
        t.stop();
        t.announce("Reloading scripts");
    }

    public void clearForwardBus() {
        forwardingBus = new EventBus();
    }

    public ScriptEngine getEngine(String engine) {
        return engines.get(engine);
    }

    public void doSetup() {
        this.setupObjects();
        this.setupSupportedEngines();
        this.setupLibraries();
    }

    public void loadScripts() {
        EventLoadScripts s = new EventLoadScripts(this);
        bus.post(s);
    }

    public void doSave() {
        EventSave s = new EventSave();
        forwardingBus.post(s);
        EventSaveComplete s2 = new EventSaveComplete(s.getMap());
        bus.post(s2);
    }

    public void doLoad() {
        EventStartLoad s = new EventStartLoad();
        bus.post(s);
        EventLoad s2 = new EventLoad(s.getMap());
        forwardingBus.post(s2);
    }

    public Object getVariable_debug(String engine, String var) {
        return engines.get(engine).get(var);
    }

    public Invocable getInvocable(String engine) {
        return (Invocable) engines.get(engine);
    }

    private void setupLibraries() {
        this.runInternalScript("scripts/extract_imports.js");
        this.runInternalScript("scripts/globals.js");
        this.runInternalScript("scripts/globals.lua");
    }

    public void runInternalScript(String path) {
        InputStream st = this.getClass().getClassLoader().getResourceAsStream(path);
        this.importStream(st, path);
    }

    public void manuallyExposeObjectToAll(ExposedObject o) {
        this.exposedObjects.add(o);
        for (ScriptEngine e : this.engines.values()) {
            e.put(o.getIdentifier(), o.getObj());
        }
    }

    private void setupObjects() {
        exposedObjects.add(new ExposedObject("scriptingCore", this));
        bus.post(new EventExposeObjects(exposedObjects));
        //---------------
        EventSetScriptFolder event = new EventSetScriptFolder();
        bus.post(event);
        this.scriptFolder = event.getFolder();
        this.scriptCache = new File(this.scriptFolder, "cache");
        this.scriptCache.mkdirs();
        exposedObjects.add(new ExposedObject("cache", this.scriptCache));
        //----------------
        EventSetSaveFolder event2 = new EventSetSaveFolder();
        bus.post(event2);
        this.saveFolder = event2.getFolder();
        exposedObjects.add(new ExposedObject("saveFolder", this.saveFolder));
        for (File f : FileUtils.listFiles(this.scriptCache, new AgeFileFilter(new Date(1361635382096L)), null)) {
            System.out.println(f.getName() + " has not been loaded from cache in 7 days. Marking for delete.");
            f.deleteOnExit();
        }
    }

    private void setupSupportedEngines() {
        for (EnumScripting s : EnumScripting.values()) {
            if (!engines.containsKey(s)) {
                engines.put(s.getEngine(), s.getScriptEngine());
                for (ExposedObject o : exposedObjects) {
                    engines.get(s.getEngine()).put(o.getIdentifier(), o.getObj());
                }
                engines.get(s.getEngine()).put("lang", s.getEngine());
            }
        }
    }

    public void importFile(File file) {
        try {
            FileInputStream f = new FileInputStream(file);
            this.importStream(f, file.getName());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void importStream(InputStream stream, String fileName) {
        for (EnumScripting s : EnumScripting.values()) {
            if (s.isCompatible(fileName)) {
                String script = s.getHandler().Import(stream, this.scriptCache);
                try {
                    engines.get(s.getEngine()).eval(script);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}
