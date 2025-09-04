package engine.common.block;

public enum Material {
	AIR(0),
	DIRT(1),
	GRASS(2),
	STONE(3);
	
	private final int id;
	
	Material(int id) {
        this.id = id;
    }
	
	public static Material fromId(int id) {
        for (Material m : values()) {
            if (m.id == id) return m;
        }
        return AIR; // fallback if unknown
    }
	
	public int getId() {
        return id;
    }

}
