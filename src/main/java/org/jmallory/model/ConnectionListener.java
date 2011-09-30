package org.jmallory.model;

import java.awt.Component;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.jmallory.util.Utils;

public interface ConnectionListener {

    void repaint();

    void setLeft(Component left);

    void setRight(Component right);

    void start();

    void close();

    void stop();

    void newConnection(Socket inSocket);

    public static abstract class ConnectionTableModel<C extends Connection> extends
            AbstractTableModel {

        private static final long serialVersionUID = 1L;

        protected final String[]  columnName;
        protected final String[]  recentRow;

        protected List<C>         data             = new ArrayList<C>();

        public ConnectionTableModel(String[] columnName, String[] recentRow) {
            this.columnName = columnName;
            this.recentRow = recentRow;
        }

        @Override
        public int getColumnCount() {
            return columnName.length;
        }

        @Override
        public int getRowCount() {
            return data.size() + 1;
        }

        public int size() {
            return data.size();
        }

        @Override
        public String getColumnName(int column) {
            return columnName[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == 0) {
                return recentRow[columnIndex];
            } else {
                return doGetValueAt(rowIndex, columnIndex);
            }
        }

        /**
         * Promissed the rowIndex != 0, i.e., not the recent row.
         * 
         * @param rowIndex
         * @param columnIndex
         * @return
         */
        protected abstract Object doGetValueAt(int rowIndex, int columnIndex);

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public C getConnectionByRow(int row) {
            if (row == 0) {
                row = data.size() - 1;
            }
            return data.get(row - 1);
        }

        public C getConnection(int index) {
            return data.get(index);
        }

        public boolean addConnection(C conn) {
            data.add(conn);
            boolean shrinked = shrinkConnections();
            if (!shrinked) {
                this.fireTableRowsInserted(data.size(), data.size()); // plus the recent line
            }
            return shrinked;
        }

        private boolean shrinkConnections() {
            if (data.size() > Utils.CONNECTION_SHRINK_THRESHOLD) {
                int shrink = data.size() - Utils.CONNECTION_SHRINK_TO;
                List<C> newData = new ArrayList<C>();
                int i = 0;
                int count = 0;
                for (; i < data.size() && count < shrink; i++) {
                    C conn = data.get(i);
                    if (!conn.isActive()) {
                        conn.halt();
                        count++;
                    } else {
                        newData.add(conn);
                    }
                }
                // append the rest connections
                for (; i < data.size(); i++) {
                    newData.add(data.get(i));
                }
                data = newData;
                fireTableDataChanged();
                return true;
            }
            return false;
        }

        public void removeConnection(C conn) {
            int row = -1;
            for (int i = 0, length = data.size(); i < length; i++) {
                C tempConn = data.get(i);
                if (tempConn == conn) {
                    data.remove(i);
                    row = i + 1;
                    break;
                }
            }

            if (row != -1) {
                // we removed one
                fireTableRowsDeleted(row, row); // plus the recent line                
            }
        }

        public void removeConnectionByRow(int row) {
            if (row > 0) {
                int index = row - 1;
                data.remove(index).halt(); // halt the conn
                fireTableRowsDeleted(row, row);
            }
        }

        public void haltConnectionByRow(int row) {
            if (row > 0) {
                int index = row - 1;
                data.get(index).halt(); // halt the conn
                fireTableRowsUpdated(row, row);
            }
        }
        
        public void removeAllConnection() {
            for (int i = 0, length = data.size(); i < length; i++) {
                data.get(i).halt();
            }
            data.clear();
            fireTableDataChanged();
        }

        public C lastConnection() {
            return data.get(data.size() - 1);
        }

        public void fireTableRowsUpdated(C connection) {
            int index = data.indexOf(connection);
            if (index != -1) {
                fireTableRowsUpdated(index + 1, index + 1);
            }
        }
    }
}
