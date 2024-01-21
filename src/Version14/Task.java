package Version14;

import battlecode.common.MapLocation;

public class Task
{
    public MapLocation location;
    public boolean builderDispatched;

    public int arrayIndex;

    public Task(MapLocation location, boolean builderDispatched, int arrayIndex ) {
        this.location = location;
        this.builderDispatched = builderDispatched;
        this.arrayIndex = arrayIndex;
    }

    public Task(MapLocation location, boolean builderDispatched)
    {
        this.location = location;
        this.builderDispatched = builderDispatched;
    }

    @Override
    public String toString() {
        return "Task{" +
                "location=" + location +
                ", builderDispatched=" + builderDispatched +
                ", arrayIndex=" + arrayIndex +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        return ((Task) obj).location.equals(this.location);
    }
}
