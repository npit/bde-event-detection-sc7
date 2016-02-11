/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.structs;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class LocSched {

    private final long schedule_id;
    private long last_parsed;

    public LocSched(long schedule_id) {
        this.schedule_id = schedule_id;
    }

    public LocSched(long schedule_id, long last_parsed) {
        this.schedule_id = schedule_id;
        this.last_parsed = last_parsed;
    }

    public long getLast_parsed() {
        return last_parsed;
    }

    public void setLast_parsed(long last_parsed) {
        this.last_parsed = last_parsed;
    }

    public long getSchedule_id() {
        return schedule_id;
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
