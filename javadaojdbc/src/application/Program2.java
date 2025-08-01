package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import model.dao.DaoFactory;
import model.dao.DepartmentDao;
import model.entities.Department;

public class Program2 {

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		DepartmentDao departmentDao = DaoFactory.createDepartmentDao();
		
		System.out.println("===TEST 1: department findById ===");
		Department department = departmentDao.findById(3);
		System.out.println(department);
		
		System.out.println("\n=== TEST 2: department findAll ===");
		List<Department> list = new ArrayList<>();
		list = departmentDao.findAll();
		for (Department dep : list) {
			System.out.println(dep);
		}
		
		System.out.println("\n=== TEST 3: department insert");
		Department newDepartment = new Department(null, "Music");
		departmentDao.insert(newDepartment);
		System.out.printf("Inserted! New id: " + newDepartment.getId());
		
		System.out.println("\n=== TEST 4: department update");
		department = departmentDao.findById(7);
		department.setName("Audiobooks");
		departmentDao.update(department);
		System.out.println("Updated successfully");
		
		System.out.println("\n=== TEST 5: department deleteById");
		System.out.printf("Insert the id for the deletion test:");
		int objId = sc.nextInt();
		departmentDao.deleteById(objId);
		System.out.println("Deletion complete");
		sc.close();
	}

}
