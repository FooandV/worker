package main

import (
	"encoding/json"
	"net/http"
)

type Product struct {
	ProductID   string  `json:"productId"`
	Name        string  `json:"name"`
	Description string  `json:"description"`
	Price       float64 `json:"price"`
}

type Customer struct {
	CustomerID string `json:"customerId"`
	Name       string `json:"name"`
	Email      string `json:"email"`
	Active     bool   `json:"active"`
}

func getProduct(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	productID := r.URL.Query().Get("productId")

	var product Product
	if productID == "product-100" {
		product = Product{
			ProductID:   "product-100",
			Name:        "Iphone",
			Description: "Un smartphone de última generación",
			Price:       2000.00,
		}
	} else {
		product = Product{
			ProductID:   "product-789",
			Name:        "Laptop",
			Description: "Una laptop potente",
			Price:       999.99,
		}
	}
	json.NewEncoder(w).Encode(product)
}

func getCustomer(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json") // Asegurarse de que se devuelva como JSON

	// Obtener el parámetro customerId de la query string
	customerID := r.URL.Query().Get("customerId")

	// Simular diferentes clientes según el customerId recibido
	var customer Customer
	if customerID == "Freyder-111" {
		customer = Customer{
			CustomerID: "Freyder-111",
			Name:       "Freyder Otalvaro",
			Email:      "freyde.otalvaro@example.com",
			Active:     false, // Cliente inactivo
		}
	} else {
		// Cliente por defecto
		customer = Customer{
			CustomerID: "Violetta-495",
			Name:       "Violetta Otalvaro",
			Email:      "violetta.otalvaro@example.com",
			Active:     true,
		}
	}

	// Devolver el cliente como respuesta
	json.NewEncoder(w).Encode(customer)
}

func main() {
	http.HandleFunc("/product", getProduct)
	http.HandleFunc("/customer", getCustomer)

	// Iniciar el servidor en el puerto 8081
	http.ListenAndServe(":8081", nil)
}
