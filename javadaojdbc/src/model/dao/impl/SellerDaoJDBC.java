package model.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import db.DB;
import db.DbException;
import model.dao.SellerDao;
import model.entities.Department;
import model.entities.Seller;

public class SellerDaoJDBC implements SellerDao {

	// Nosso DAO terá uma dependência com a conexão:
	private Connection conn;
	
	// E então forçar a injeção de dependência aqui dentro
	public SellerDaoJDBC(Connection conn) {
		this.conn = conn;
	}
	
	@Override
	public void insert(Seller obj) {

		PreparedStatement st = null;
		try {
			st = conn.prepareStatement(
					"INSERT INTO seller "
					+ "(Name, Email, BirthDate, BaseSalary, DepartmentId) "
					+ "VALUES "
					+ "(?, ?, ?, ?, ?)", 
					Statement.RETURN_GENERATED_KEYS); // retorna o ID inserido
			
			st.setString(1, obj.getName());
			st.setString(2, obj.getEmail());
			st.setDate(3, new java.sql.Date(obj.getBirthDate().getTime()));
			st.setDouble(4, obj.getBaseSalary());
			st.setInt(5, obj.getDepartment().getId());
			
			int rowsAffected = st.executeUpdate();
			
			if(rowsAffected > 0) {
				ResultSet rs = st.getGeneratedKeys();
				if (rs.next()) {
					int id = rs.getInt(1);
					obj.setId(id);
				}
				DB.closeResultSet(rs);
			} 
			else {
				throw new DbException("Unexpected error! No rows affected");
			}
			
		}
		catch (SQLException e) {
			throw new DbException(e.getMessage());
		}
		finally {
			DB.closeStatement(st);
		}
		
	}

	@Override
	public void update(Seller obj) {

		PreparedStatement st = null;
		
		try {
			st = conn.prepareStatement(
					"UPDATE seller "
					+ "SET Name = ?, Email = ?, BirthDate = ?, BaseSalary = ?, DepartmentId = ? "
					+ "WHERE Id = ?");
			
			st.setString(1, obj.getName());
			st.setString(2, obj.getEmail());
			st.setDate(3, new java.sql.Date(obj.getBirthDate().getTime()));
			st.setDouble(4, obj.getBaseSalary());
			st.setInt(5, obj.getDepartment().getId());
			st.setInt(6, obj.getId());
			
			st.executeUpdate();
		}
		catch (SQLException e) {
			throw new DbException(e.getMessage());
		}
		finally {
			DB.closeStatement(st);
		}
	}

	@Override
	public void deleteById(Integer id) {
		PreparedStatement st = null;
		
		try {
			st = conn.prepareStatement(
					"DELETE FROM seller "
					+ "WHERE Id = ?");
			
			st.setInt(1, id);
			int rowsAffected = st.executeUpdate();
			
			if (rowsAffected == 0) {
				throw new DbException("Error! Couldn't delete the id");
			}
			
		}
		catch (SQLException e) {
			throw new DbException(e.getMessage());
		}
		finally {
			DB.closeStatement(st);
		}
	}

	@Override
	public Seller findById(Integer id) {
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement(
					"SELECT seller.*, department.name as DepName "
					+ "FROM seller INNER JOIN department "
					+ "ON seller.DepartmentId = department.Id "
					+ "WHERE seller.id = ?");
			
			st.setInt(1, id);
			rs = st.executeQuery();
			// Quando executamos uma query, e vem o resultado no Resultset, ele aponta 
			// para a posição 0, e esta não contém objeto, só na 1:
			if (rs.next()) { 
				Department dep = instantiateDepartment(rs);
				Seller obj = instantiateSeller(rs, dep);
				return obj;
			}
			
			return null; // Ou seja não existia seller com esse id
		}
		catch (SQLException e) {
			throw new DbException(e.getMessage());
		}
		finally {
			DB.closeStatement(st);
			DB.closeResultSet(rs);
		}
	}

	private Seller instantiateSeller(ResultSet rs, Department dep) throws SQLException {
		Seller obj = new Seller();
		obj.setId(rs.getInt("Id"));
		obj.setName(rs.getString("Name"));
		obj.setEmail(rs.getString("Email"));
		obj.setBaseSalary(rs.getDouble("BaseSalary"));
		obj.setBirthDate(rs.getDate("BirthDate"));
		obj.setDepartment(dep);
		return obj;
	}

	private Department instantiateDepartment(ResultSet rs) throws SQLException {
		Department dep = new Department();
		dep.setId(rs.getInt("DepartmentId"));
		dep.setName(rs.getString("DepName"));
		return dep;
	}

	@Override
	public List<Seller> findAll() {
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement(
					"SELECT seller.*,department.Name AS DepName "
					+ "FROM seller INNER JOIN department "
					+ "ON seller.DepartmentId = department.Id "
					+ "ORDER BY Name");
			
			rs = st.executeQuery();
			
			List<Seller> list = new ArrayList<>();
			Map<Integer, Department> map = new HashMap<>();
			
			while(rs.next()) {
				Department dep = map.get(rs.getInt("DepartmentId"));
				
				if(dep == null) {
					dep = instantiateDepartment(rs);
					map.put(rs.getInt("DepartmentId"), dep);
				}
				
				Seller obj = instantiateSeller(rs, dep);
				list.add(obj);
			}
			return list;
		}
		catch (SQLException e) {
			throw new DbException(e.getMessage());
		}
		finally {
			DB.closeStatement(st);
			DB.closeResultSet(rs);
		}
	}

	@Override
	public List<Seller> findByDeparment(Department department) {
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = conn.prepareStatement(
					"SELECT seller.*, department.Name AS DepName "
					+ "FROM seller INNER JOIN department "
					+ "ON seller.DepartmentId = department.Id "
					+ "WHERE DepartmentId = ? "
					+ "ORDER BY Name");
			st.setInt(1, department.getId());
			
			rs = st.executeQuery();
			
			// Como podem ter de 0 a mais valores, não pode ser um if, necessitando ser 
			// um while, percorrendo enquanto tiver um próximo
			List<Seller> list = new ArrayList<>();
			
			// Controlando para não repetir o departamento
			Map<Integer, Department> map = new HashMap<>();
						
			while (rs.next()) {
				
				// Checa se já não existe um DepartmentId
				Department dep = map.get(rs.getInt("DepartmentId"));
				
				// Se não tiver, instancia-o 
				if (dep == null) {
					dep = instantiateDepartment(rs);
					map.put(rs.getInt("DepartmentId"), dep);
				}
				
				Seller obj = instantiateSeller(rs, dep);
				list.add(obj);
			}
			return list;
		}
		catch (SQLException e) {
			throw new DbException(e.getMessage());
		}
		finally {
			DB.closeStatement(st);
			DB.closeResultSet(rs);
		}
		
	}

}
