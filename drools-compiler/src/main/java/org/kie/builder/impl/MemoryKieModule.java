package org.kie.builder.impl;

import org.drools.commons.jci.readers.ResourceReader;
import org.drools.compiler.io.memory.MemoryFileSystem;
import org.drools.kproject.models.KieModuleModelImpl;
import org.kie.builder.GAV;
import org.kie.builder.KieModuleModel;

import java.io.File;
import java.util.Collection;

public class MemoryKieModule extends AbstractKieModule implements ResourceReader {

    private final MemoryFileSystem mfs;
    
    public MemoryKieModule(GAV gav) {
        this(gav, new KieModuleModelImpl(), new MemoryFileSystem());
    }

    public MemoryKieModule(GAV gav, KieModuleModel kieProject, MemoryFileSystem mfs) {
        super(gav, kieProject);
        this.mfs = mfs;
    }
    
    @Override
    public boolean isAvailable(String path) {
        return mfs.existsFile( path );
    }

    @Override
    public byte[] getBytes(String path) {
        return mfs.getBytes( path );
    }

    @Override
    public Collection<String> getFileNames() {
        return mfs.getFileNames();
    }
    
    public MemoryFileSystem  getMemoryFileSystem() {
        return this.mfs;
    }

    @Override
    public File getFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getBytes() {
        return mfs.writeAsBytes();
    }
    
    public String toString() {
        return "MemoryKieModule[ GAV=" + getGAV() + "]";
    }
}
