package Version8Water;

import battlecode.common.MapLocation;

public class Flag
{
    public MapLocation location;
    public boolean isStolen;

    public Flag (MapLocation location, boolean isStolen)
    {
        this.location = location;
        this.isStolen = isStolen;
    }
}
