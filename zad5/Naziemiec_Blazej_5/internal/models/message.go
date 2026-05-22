package models

type Message struct {
	Type        string `json:"type"`
	AgencyName  string `json:"agency_name,omitempty"`
	OrderID     string `json:"order_id,omitempty"`
	ServiceType string `json:"service_type,omitempty"`
	CarrierName string `json:"carrier_name,omitempty"`
	Content     string `json:"content,omitempty"`
}
