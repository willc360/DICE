/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dicetrackeranalysis.graphhandling;

import java.util.LinkedList;

/**
 *
 * @author lee
 */
public class TaintedObject extends RecordSetter {

    private String taintID;
    private String taintRecord;
    private String type;
    private String objectID;
    private String value;
    private boolean unused;
    private LinkedList<TaintedObject> subTaintedObjects;

    public TaintedObject() {
        subTaintedObjects = new LinkedList<TaintedObject>();
        unused = false;
    }

    public void setTaintID(String taintID) {
        this.taintID = setRecord(taintID);
    }

    public void setTaintRecord(String taintRecord) {
        this.taintRecord = setRecord(taintRecord);
    }

    public void setType(String type) {
        this.type = setRecord(type);
    }

    public void setObjectID(String objectID) {
        this.objectID = setRecord(objectID);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setUnused() {
        this.unused = true;
    }

    public void addTaintedObject(TaintedObject object) {
        this.subTaintedObjects.add(object);
    }

    public String getTaintID() {
        return this.taintID;
    }

    public String getValue() {
        return this.value;
    }

    public String getTaintRecord() {
        return this.taintRecord;
    }

    public String getType() {
        return this.type;
    }

    public String getObjectID() {
        return this.objectID;
    }

    public boolean isUnused() {
        return this.unused;
    }

    public LinkedList<TaintedObject> getSubTaintedObjects() {
        return this.subTaintedObjects;
    }

}