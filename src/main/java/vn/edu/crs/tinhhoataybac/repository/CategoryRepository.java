package vn.edu.crs.tinhhoataybac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.crs.tinhhoataybac.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}