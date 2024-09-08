package com.bbn.marti.util.missionpackage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

public class MissionPackage {

    private String filename;
    private ByteArrayOutputStream bos;
    private ZipOutputStream zos;
    MissionPackageManifest manifest;

    public byte[] getByteArray() { return bos.toByteArray(); }

    public MissionPackage(String filename) throws IOException, JAXBException {
        this.filename = filename;
        this.bos = new ByteArrayOutputStream();
        this.zos = new ZipOutputStream(bos);

        manifest = new MissionPackageManifest();
        manifest.setVersion("2");
        ConfigurationType configurationType = new ConfigurationType();
        manifest.setConfiguration(configurationType);
        ContentsType contentsType = new ContentsType();
        manifest.setContents(contentsType);
    }
    
    public void addPermission(String name) {
        PermissionType permissionType = new PermissionType();
        permissionType.setName(name);

        if (manifest.getRole() == null) {
            manifest.setRole(new RoleType());
        }

        manifest.getRole().getPermission().add(permissionType);
    }
    
    public void addRoleName(String name) {
        if (manifest.getRole() == null) {
            manifest.setRole(new RoleType());
        }

        manifest.getRole().setName(name);
    }
    
    public void addGroup(String name) {
        GroupType groupType = new GroupType();
        groupType.setName(name);

        if (manifest.getGroups() == null) {
            manifest.setGroups(new GroupsType());
        }

        manifest.getGroups().getGroup().add(groupType);
    }

    public void addParameter(String name, String value) {
        ParameterType parameterType = new ParameterType();
        parameterType.setName(name);
        parameterType.setValue(value);
        manifest.getConfiguration().getParameter().add(parameterType);
    }

    private void addEntry(String filename, byte[] contents) throws IOException {
        ZipEntry newEntry = new ZipEntry(filename);
        newEntry.setTime(0);
        zos.putNextEntry(newEntry);
        if (contents != null) {
            zos.write(contents);
        }
        zos.closeEntry();
    }
    
    public ContentType addFile(String filename, byte[] contents) throws IOException {
        return addContentFile(filename, contents, new ContentType());
    }

    public ContentType addContentFile(String filename, byte[] contents, ContentType contentType) throws IOException {
        addEntry(filename, contents);

        contentType.setZipEntry(filename);
        contentType.setIgnore(false);
        manifest.getContents().getContent().add(contentType);
        return contentType;
    }

    public void addCotFile(String filename, byte[] contents, String uid) throws IOException {
    	ContentType contentType = addContentFile(filename, contents, new ContentType());

        ParameterType parameterType = new ParameterType();
        parameterType.setName("uid");
        parameterType.setValue(uid);
        contentType.setParameter(parameterType);
    }

    public void addDirectory(String dirname) throws IOException {
        addEntry(dirname, null);
    }

    public byte[] save() throws IOException, JAXBException {
        JAXBContext jc = JAXBContext.newInstance(MissionPackageManifest.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(manifest, writer);
        String manifest = writer.toString();

        addDirectory("MANIFEST/");
        addFile("MANIFEST/manifest.xml", manifest.getBytes());

        zos.finish();
        zos.close();
        bos.flush();
        bos.close();

        return getByteArray();
    }

    public static HashMap<String, byte[]> extractMissionPackage(byte[] missionPackage) throws IOException {
        HashMap<String, byte[]> files = new HashMap<>();

        ZipEntry entry;
        final int BUFFER = 2048;

        ByteArrayInputStream bis = new ByteArrayInputStream(missionPackage);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bis));
        while ((entry = zis.getNextEntry()) != null) {

            // skip directories
            if (entry.isDirectory()) {
                continue;
            }

            // load in the file
            int count;
            byte data[] = new byte[BUFFER];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while ((count = zis.read(data, 0, BUFFER)) != -1) {
                bos.write(data, 0, count);
            }
            bos.flush();
            bos.close();

            String filename = entry.getName();
            filename = filename.substring(filename.lastIndexOf("/") + 1);
            files.put(filename, bos.toByteArray());
        }
        zis.close();

        return files;
    }
}
