/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.structs;

import gr.demokritos.iit.location.mode.DocumentMode;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocSched {

    private final long schedule_id;
    private final DocumentMode operation_mode;
    private long last_parsed;
    private long items_updated;

    public LocSched(DocumentMode operation_mode, long schedule_id, long last_parsed) {
        this.operation_mode = operation_mode;
        this.schedule_id = schedule_id;
        this.last_parsed = last_parsed;
    }

    public long getLastParsed() {
        return last_parsed;
    }

    public DocumentMode getOperationMode() {
        return operation_mode;
    }

    public void setLastParsed(long last_parsed) {
        this.last_parsed = last_parsed;
    }

    public long getScheduleID() {
        return schedule_id;
    }

    public long getItemsUpdated() {
        return items_updated;
    }

    public void setItemsUpdated(long items_updated) {
        this.items_updated = items_updated;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + (int) (this.schedule_id ^ (this.schedule_id >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LocSched other = (LocSched) obj;
        if (this.schedule_id != other.schedule_id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LocSched<" + "schedule_id: " + schedule_id + ", last_parsed: " + last_parsed + ">";
    }

}
