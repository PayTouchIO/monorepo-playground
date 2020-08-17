require 'pt_client'
require 'faker'

module Seeds
  def self.run!(admin_username, admin_password)
    first_name = Faker::Name.first_name
    last_name = Faker::Name.last_name
    suffix = [first_name, last_name].join('-').downcase
    options = {
      first_name: first_name,
      last_name: last_name,
      email: "ordering.#{suffix}@example.com",
      password: "o",
    }

    location_name = Faker::Address.street_address

    test_username, test_password = PtClient::Merchants.create(admin_username, admin_password, location_name, options)

    # The first location `Location 1` is created when creating the merchant.
    location1 = PtClient::Locations.find_by_name(location_name)
    PtClient::Locations.update(location1.id, {
      name: location1.name,
      address: {
        line1: '2200 Central Drive',
        line2: '',
        city: 'Bedford',
        state: 'TX',
        postal_code: '76021',
        country: 'US'
      },
      opening_hours: {
        monday: [{
          start: "00:00",
          end: "23:59"
        }],
        tuesday: [{
          start: "00:00",
          end: "23:59"
        }],
        wednesday: [{
          start: "00:00",
          end: "23:59"
        }],
        thursday: [{
          start: "00:00",
          end: "23:59"
        }],
        friday: [{
          start: "00:00",
          end: "23:59"
        }],
        saturday: [{
          start: "00:00",
          end: "23:59"
        }],
        sunday: [{
          start: "00:00",
          end: "23:59"
        }]
      }
    })

    category = PtClient::Categories.create(name: "All", locations: [location1])

    taxRateApplyA = PtClient::TaxRates.create(name: 'ApplyA', value: 25, apply_to_price: true, locations: [location1])
    taxRateApplyB = PtClient::TaxRates.create(name: 'ApplyB', value: 12, apply_to_price: true, locations: [location1])
    taxRateIncludedA = PtClient::TaxRates.create(name: 'IncludedA', value: 5,  apply_to_price: false,locations: [location1])
    taxRateIncludedB = PtClient::TaxRates.create(name: 'IncludedB', value: 10, apply_to_price: false, locations: [location1])

    modifierSetAdd = PtClient::Modifiers.create(
        name: 'MyAdd',
        type: 'addon',
        single_choice: true,
        force: true,
        options: [
            {
              price: {
                currency: 'USD',
                amount: 1.5
              },
              position: 0,
              active: true,
              name: 'MyAddA',
              id: SecureRandom.uuid
            },
            {
              price: {
                currency: 'USD',
                amount: 1.25
              },
              position: 1,
              active: true,
              name: 'MyAddB',
              id: SecureRandom.uuid
            }
        ],
        locations: [location1])

    modifierSetHold = PtClient::Modifiers.create(
        name: 'MyHold',
        type: 'hold',
        single_choice: true,
        force: true,
        options: [
            {
              price: {
                currency: 'USD',
                amount: 0.25
              },
              position: 0,
              active: true,
              name: 'MyHoldA',
              id: SecureRandom.uuid
            },
            {
              price: {
                currency: 'USD',
                amount: 0.35
              },
              position: 1,
              active: true,
              name: 'MyHoldB',
              id: SecureRandom.uuid
            }
        ],
        locations: [location1])

    modifierSetNeutral = PtClient::Modifiers.create(
        name: 'MyNeutral',
        type: 'neutral',
        single_choice: true,
        force: true,
        options: [
            {
              price: {
                currency: 'USD',
                amount: 0
              },
              position: 0,
              active: true,
              name: 'MyNeutralA',
              id: SecureRandom.uuid
            },
            {
              price: {
                currency: 'USD',
                amount: 0
              },
              position: 1,
              active: true,
              name: 'MyNeutralB',
              id: SecureRandom.uuid
            }
        ],
        locations: [location1])

    def self.create_product(name, price, cost, category, location, tax_rates, modifier_sets)
        product = PtClient::Products.create(
          location,
          {
            name: name,
            category_ids: [category.id],
            price: {
              amount: price,
              currency: "USD"
            },
            cost: {
              amount: cost,
              currency: "USD"
            },
            location_overrides: {
              location.id => {
                price: {
                  amount: price,
                  currency: "USD"
                },
                cost: {
                  amount: cost,
                  currency: "USD"
                },
                margin: "50.0",
                unit: "unit",
                active: true,
                tax_rate_ids: tax_rates.map { |t| t.id }
              }
            }
          }
        )
        PtClient::Products.assign_modifier_sets(product.id, modifier_sets.map { |m| m.id })
        product
    end

    def self.create_bundle(name, price, cost, category, location, tax_rates, modifier_sets, bundle_sets)
        product = PtClient::Bundles.create(
          location,
          bundle_sets,
          {
            name: name,
            category_ids: [category.id],
            price: {
              amount: price,
              currency: "USD"
            },
            cost: {
              amount: cost,
              currency: "USD"
            },
            location_overrides: {
              location.id => {
                price: {
                  amount: price,
                  currency: "USD"
                },
                cost: {
                  amount: cost,
                  currency: "USD"
                },
                margin: "50.0",
                unit: "unit",
                active: true,
                tax_rate_ids: tax_rates.map { |t| t.id }
              }
            }
          }
        )
        PtClient::Products.assign_modifier_sets(product.id, modifier_sets.map { |m| m.id })
        product
    end

    create_product(
      name = "Scenario [A]",
      price = 1,
      cost = 0.5,
      category = category,
      location = location1,
      tax_rates = [taxRateApplyA, taxRateApplyB],
      modifier_sets = [modifierSetAdd, modifierSetHold]
    )

    create_product(
      name = "Scenario [B]",
      price = 1,
      cost = 0.5,
      category = category,
      location = location1,
      tax_rates = [taxRateApplyA, taxRateApplyB, taxRateIncludedA, taxRateIncludedB],
      modifier_sets = [modifierSetAdd, modifierSetHold]
    )

    create_product(
      name = "Scenario [C] - 1",
      price = 1,
      cost = 0.5,
      category = category,
      location = location1,
      tax_rates = [taxRateApplyA, taxRateApplyB],
      modifier_sets = [modifierSetAdd, modifierSetHold]
    )

    create_product(
      name = "Scenario [C] - 2",
      price = 1,
      cost = 0.5,
      category = category,
      location = location1,
      tax_rates = [taxRateApplyA, taxRateApplyB],
      modifier_sets = [modifierSetAdd, modifierSetHold]
    )

    create_product(
      name = "Scenario [D]",
      price = 1,
      cost = 0.5,
      category = category,
      location = location1,
      tax_rates = [taxRateApplyA, taxRateApplyB],
      modifier_sets = [modifierSetNeutral]
    )

    bundle_product_1 = create_product(
      name = "Scenario [E] - product 1 for bundle",
      price = 1,
      cost = 0.5,
      category = category,
      location = location1,
      tax_rates = [taxRateApplyA],
      modifier_sets = [modifierSetAdd]
    )

    bundle_product_2 = create_product(
      name = "Scenario [E] - product 2 for bundle",
      price = 1,
      cost = 0.5,
      category = category,
      location = location1,
      tax_rates = [taxRateApplyA],
      modifier_sets = []
    )

    create_bundle(
      name = "Scenario [E] - bundle product",
      price = 2,
      cost = 1,
      category = category,
      location = location1,
      tax_rates = [taxRateApplyA],
      modifier_sets = [],
      bundle_sets = [
        {
            id: SecureRandom.uuid,
            name: "MyBundleSet",
            position: 1,
            min_quantity: 1,
            max_quantity: 1,
            options: [
                {
                    id: SecureRandom.uuid,
                    article_id: bundle_product_1.id,
                    price_adjustment: 0
                },
                {
                    id: SecureRandom.uuid,
                    article_id: bundle_product_2.id,
                    price_adjustment: 5
                }
            ]
        }
      ]
    )

    gift_card = PtClient::GiftCards.create()

    return test_username, test_password
  end

  PtClient.setup!(env: ENV['ENV'] || :dev)
  run!("marco@paytouch.io", "Paytouch2016")
end
