package at.fraihs.cookoff.cookoff.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plate_colors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlateColorJpaEntity {

    @Id
    private Long id;

    private String name;

    @Column(name = "hex_code")
    private String hexCode;

    @Column(name = "sort_order")
    private int sortOrder;

    private boolean active;
}
