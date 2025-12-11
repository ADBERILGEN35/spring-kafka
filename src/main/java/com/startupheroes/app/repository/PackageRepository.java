package com.startupheroes.app.repository;

import com.startupheroes.app.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {

    /**
     * Not – Yüksek hacimli veri senaryosu:
     * Bu metot tüm sonuçları tek seferde belleğe yükleyen bir List döndürür.
     * Orta ölçekli veri setleri için uygundur; ancak milyonlarca kayıt içeren
     * büyük hacimli tablolarda bellek kullanımı ve sorgu süresi açısından
     * verimli olmayabilir.
     *
     * Production ortamında böyle bir ihtiyaç doğarsa:
     * - Pageable ile veriyi sayfa sayfa çekmek (örn. findByCancelledFalse(Pageable)),
     * - veya JPA Stream kullanarak veriyi streaming şekilde tüketmek,
     * genellikle daha uygun yaklaşımlar olacaktır.
     */
    List<Package> findAllByCancelledFalse();

}
