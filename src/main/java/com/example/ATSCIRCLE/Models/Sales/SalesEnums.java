package com.example.ATSCIRCLE.Models.Sales;

/**
 * Shared enums for Sales module (Company, Client, etc.)
 * This avoids duplication and type conversion issues
 */
public class SalesEnums {

    public enum Industry {
        IT, FINANCE, HEALTHCARE, EDUCATION, MANUFACTURING, RETAIL, OTHER
    }

    public enum RelationshipType {
        CLIENT, PARTNER, SUPPLIER, VENDOR, DISTRIBUTOR, RESELLER,
        CONTRACTOR, CONSULTANT, SERVICE_PROVIDER, INVESTOR,
        MANUFACTURER, OTHER
    }

    public enum LeadStatus {
        NEW, CONTACTED, QUALIFIED, LOST, WON, OTHER
    }

    public enum LeadSource {
        WEBSITE, REFERRAL, PARTNER, SOCIAL_MEDIA, EMAIL, CALL, OTHER
    }

    public enum CompanyStage {
        STARTUP, SMALL, MEDIUM, LARGE, ENTERPRISE, OTHER
    }

    public enum LegalType {
        SOLE_PROPRIETORSHIP, PARTNERSHIP_FIRM, LLP, PRIVATE_LIMITED,
        PUBLIC_LIMITED, OPC, JOINT_VENTURE, NON_PROFIT, GOVERNMENT_ENTITY, OTHER
    }

    public enum EmployeeCount {
        RANGE_1_10, RANGE_11_50, RANGE_51_100, RANGE_101_250,
        RANGE_251_500, RANGE_501_1000, RANGE_1001_5000,
        RANGE_5001_10000, RANGE_10000_PLUS
    }

    public enum AnnualRevenue {
        BELOW_1M, RANGE_1M_5M, RANGE_5M_10M, RANGE_10M_50M,
        RANGE_50M_100M, RANGE_100M_500M, RANGE_500M_1B,
        RANGE_1B_5B, RANGE_5B_10B, ABOVE_10B
    }

    public enum TaxIdType {
        GST, EIN, VAT, TRN, TIN, ABN, CIF, CNPJ, TVA,
        IVA, BTW, CUIT, RFC, RUC, NIT, CPF, RIF, OTHER
    }

    public enum PaymentTerms {
        NET15, NET30, NET45, NET60, NET90,
        COD, PREPAID, LC, PAYMENT_ON_RECEIPT,
        MONTHLY, QUARTERLY, ANNUAL, OTHER
    }

    public enum CurrencyType {
        INR, USD, EUR, GBP, JPY, CNY, CAD, AUD, CHF,
        SEK, NOK, DKK, SGD, HKD, NZD, ZAR, BRL,
        MXN, AED, SAR, KRW, THB, MYR, PHP, IDR,
        VND, EGP, NGN, KES, OTHER
    }
}