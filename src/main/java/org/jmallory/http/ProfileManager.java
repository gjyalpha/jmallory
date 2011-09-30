package org.jmallory.http;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmallory.util.Utils;

public class ProfileManager {

    private static List<Profile> profileList;

    public static synchronized List<Profile> reloadAllProfile() {
        List<Profile> result = new ArrayList<Profile>();

        File profileDir = getProfileDir();

        File[] profileFiles = profileDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".amp");
            }
        });

        for (File profileFile : profileFiles) {
            Profile p = new Profile(profileFile);
            result.add(p);
        }
        Collections.sort(result);
        profileList = result;
        return result;
    }

    public static synchronized Profile getProfile(String name) {
        Profile profile = findExistProfile(name);
        if (profile == null) {
            profile = createProfile(name);
            profileList.add(profile);
            Collections.sort(profileList);
        }
        return profile;
    }

    public static synchronized List<Profile> getProfileList() {
        if (profileList == null) {
            reloadAllProfile();
        }
        return profileList;
    }

    public static void deleteProfile(String name) {
        for (int i = 0, length = profileList.size(); i < length; i++) {
            if (profileList.get(i).getName().equals(name)) {
                profileList.remove(i);
                break;
            }
        }
        File profileFile = new File(getProfileDir(), name + ".amp");
        if (profileFile.exists()) {
            profileFile.delete();
        }

        File profileTxtFile = new File(getProfileDir(), name + ".amp.txt");
        if (profileTxtFile.exists()) {
            profileTxtFile.delete();
        }
    }

    private static Profile createProfile(String name) {
        File profileFile = new File(getProfileDir(), name + ".amp");
        try {
            profileFile.createNewFile();
            Utils.chmod777(profileFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Profile(profileFile);
    }

    public static synchronized Profile findExistProfile(String name) {
        for (Profile profile : profileList) {
            if (profile.getName().equals(name)) {
                return profile;
            }
        }
        return null; // null means nothing found
    }

    private static File getProfileDir() {
        File profileDir = new File("profiles");
        if (!profileDir.exists()) {
            profileDir.mkdirs();
        }

        return profileDir;
    }
}
