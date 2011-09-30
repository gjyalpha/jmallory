package org.jmallory.http;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public class ProfileTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Name";
            case 1:
                return "Last Modified";
        }

        return null;
    }

    private List<Profile> profileList;

    public ProfileTableModel(List<Profile> profileList) {
        if (profileList == null) {
            throw new IllegalArgumentException("profileList is null");
        }
        this.profileList = profileList;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return profileList.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Profile p = profileList.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return p.getName();
            case 1:
                return p.getLastModified();
        }

        return null;
    }
}
