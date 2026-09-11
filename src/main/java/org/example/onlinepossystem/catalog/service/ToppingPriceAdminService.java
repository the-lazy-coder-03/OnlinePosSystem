package org.example.onlinepossystem.catalog.service;

import org.example.onlinepossystem.branch.api.BranchLookup;
import org.example.onlinepossystem.catalog.entity.BranchExtraPrice;
import org.example.onlinepossystem.catalog.entity.PizzaSize;
import org.example.onlinepossystem.catalog.entity.PriceCategory;
import org.example.onlinepossystem.catalog.repository.BranchExtraPriceRepository;
import org.example.onlinepossystem.catalog.repository.PizzaSizeRepository;
import org.example.onlinepossystem.catalog.repository.PriceCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToppingPriceAdminService {
    private static final Logger logger = LoggerFactory.getLogger(ToppingPriceAdminService.class);

    private final BranchLookup branchLookup;
    private final PriceCategoryRepository priceCategoryRepository;
    private final PizzaSizeRepository pizzaSizeRepository;
    private final BranchExtraPriceRepository branchExtraPriceRepository;

    public ToppingPriceAdminService(
            BranchLookup branchLookup,
            PriceCategoryRepository priceCategoryRepository,
            PizzaSizeRepository pizzaSizeRepository,
            BranchExtraPriceRepository branchExtraPriceRepository
    ) {
        this.branchLookup = branchLookup;
        this.priceCategoryRepository = priceCategoryRepository;
        this.pizzaSizeRepository = pizzaSizeRepository;
        this.branchExtraPriceRepository = branchExtraPriceRepository;
    }

    @Transactional
    public void updateToppingPrice(Integer branchId, Integer priceCategoryId,
                                   Integer pizzaSizeId, Double price, String actor) {
        branchLookup.requireById(branchId);
        PriceCategory priceCategory = priceCategoryRepository.findById(priceCategoryId)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Price category not found with ID: " + priceCategoryId));
        PizzaSize pizzaSize = pizzaSizeRepository.findById(pizzaSizeId)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Pizza size not found with ID: " + pizzaSizeId));
        BranchExtraPrice branchPrice = branchExtraPriceRepository
                .findById(new BranchExtraPrice.BranchExtraPriceId(branchId, priceCategoryId, pizzaSizeId))
                .orElse(new BranchExtraPrice(branchId, priceCategory, pizzaSize, price));
        branchPrice.setPrice(price);
        branchExtraPriceRepository.save(branchPrice);
        logger.info("Admin action=updateToppingPrice branchId={} priceCategoryId={} sizeId={} admin={}",
                branchId, priceCategoryId, pizzaSizeId, CatalogAdminSupport.actorName(actor));
    }
}
