package org.jmallory.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmallory.util.Utils;


public class Profile implements Comparable<Profile>, Serializable {
    private static final long serialVersionUID = 1L;
    private File                           profileFile;
    private String                         name;
    private Date                           lastModified;

    private List<ReqRespData>              data;
    private Map<HttpRequest, HttpResponse> dataMap;

    //    private transient boolean dirty = false;

    public Profile() {
    }

    public Profile(File profileFile) {
        this.profileFile = profileFile;
        int index = this.profileFile.getName().lastIndexOf(System.getProperty("path.separator"));
        index = index == -1 ? 0 : index;
        this.name = this.profileFile.getName().substring(index,
                this.profileFile.getName().length() - 4);
        this.lastModified = new Date(profileFile.lastModified());
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try {
            File file = profileFile;
            if (file.exists()) {
                if (file.length() != 0) {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    data = (List<ReqRespData>) ois.readObject();
                    ois.close();
                }
            } else {
                file.createNewFile();
            }

            if (data == null) {
                data = new ArrayList<ReqRespData>();
                dataMap = new HashMap<HttpRequest, HttpResponse>();
            } else {
                Collections.sort(data);
                dataMap = new HashMap<HttpRequest, HttpResponse>();
                for (ReqRespData reqRespData : data) {
                    dataMap.put(reqRespData.getRequest(), reqRespData.getResponse());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized int addData(ReqRespData newReqRespData) {
        if (newReqRespData == null) {
            return -1;
        }

        if (data == null) {
            loadData();
        }

        int index = -1;

        if (dataMap.containsKey(newReqRespData.getRequest())) {
            for (int i = 0, length = data.size(); i < length; i++) {
                if (data.get(i).getRequest().equals(newReqRespData.getRequest())) {
                    data.set(i, newReqRespData);
                    index = i;
                    break;
                }
            }
        } else {
            data.add(newReqRespData);
            index = data.size() - 1;
        }
        dataMap.put(newReqRespData.getRequest(), newReqRespData.getResponse());
        return index;
    }

    public synchronized void removeData(int index) {
        if (index < 0 || index >= data.size()) {
            return;
        }

        ReqRespData reomvedData = data.remove(index);
        dataMap.remove(reomvedData.getRequest());
    }

    public synchronized void removeData(ReqRespData reqRespData) {
        data.remove(reqRespData);
        dataMap.remove(reqRespData.getRequest());
    }

    public synchronized void removeAllData() {
        data.clear();
        dataMap.clear();
    }

    public synchronized void save() {
        File file = profileFile;
        Utils.chmod777(file);
        
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(this.data);
            oos.flush();
            oos.close();

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                    new File(file.getPath() + ".txt"))));

            for (ReqRespData reqRespData : this.data) {
                writer.println("/==================== Request =====================\\");
                writer.println(reqRespData.getRequest().toString());
                writer.println("===================== Response =====================");
                writer.println(reqRespData.getResponse().toString());
                writer.println("\\==================================================/");
                writer.println();
                writer.println();
            }

            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public File getProfileFile() {
        return profileFile;
    }

    public synchronized List<ReqRespData> getData() {
        if (data == null) {
            loadData();
        }
        return data;
    }

    public synchronized Map<HttpRequest, HttpResponse> getDataMap() {
        if (data == null) {
            loadData();
        }
        return dataMap;
    }

    @Override
    public int compareTo(Profile o) {
        return name.compareTo(o.getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Profile)) {
            return false;
        }

        return name.equals(((Profile) obj).name);
    }

}
