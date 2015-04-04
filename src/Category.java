import java.text.DecimalFormat;


public class Category {
	public int cant=0;
	public String name="";
	public double cost=0;
	
	public Category(String name, double cost) {
		super();
		this.name = name;
		this.cost = cost;
		cant=0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Category other = (Category) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("0.00");
		return cant+"x "+name+" $"+df.format(cost);
	}
}
