package engine.common.block;

public class Block {
    private Material type;
    public Block(Material type) { this.type = type; }
    public Material getType() { return type; }
    public void setType(Material type) { this.type = type; }
	public boolean isSolid() {
		if (this.type == Material.AIR) return false;
		return true;
	}
}