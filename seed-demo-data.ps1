# seed-demo-data.ps1
# Seeds demo data into the running java-api (H2 in-memory) so the Nuxt frontend
# has something to display. Run from PowerShell:  powershell -File seed-demo-data.ps1
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8082/api/v1'

function Post($path, $body) {
    Invoke-RestMethod -Method Post -Uri ($base + $path) -ContentType 'application/json' -Body ($body | ConvertTo-Json -Depth 6)
}

Write-Host 'Seeding demo data...'

# 1) Users (buyer, seller, admin)
$buyer  = Post '/users/register' @{ email = 'buyer@demo.com';  password = '12345678'; fullName = 'Demo Buyer';  phone = '0123456789' }
$seller = Post '/users/register' @{ email = 'seller@demo.com'; password = '12345678'; fullName = 'Demo Seller'; phone = '0987654321' }
$admin  = Post '/users/register' @{ email = 'admin@demo.com';  password = '12345678'; fullName = 'Demo Admin';  phone = '0111222333' }
Write-Host ("User IDs - buyer=$($buyer.id) seller=$($seller.id) admin=$($admin.id)")

# Bootstrap: no admin exists yet, so the first admin grants itself the role.
# adminUserId isn't verified server-side (see java-api's CLAUDE.md) - same as every other admin call below.
Post ("/users/$($admin.id)/admin-role?adminUserId=$($admin.id)") @{} | Out-Null

# Personal admin account (kemheng) - granted by the demo admin above.
$kemheng = Post '/users/register' @{ email = 'kemheng.cheng@chipmong.com'; password = '12345678'; fullName = 'Kemheng Cheng' }
Post ("/users/$($kemheng.id)/admin-role?adminUserId=$($admin.id)") @{} | Out-Null
Write-Host ("Personal admin ID=$($kemheng.id) email=$($kemheng.email)")

# 2) Categories
$catElec = Post '/categories' @{ categoryName = 'Electronics'; categoryDescription = 'Gadgets and devices'; displayOrder = 1 }
$catHome = Post '/categories' @{ categoryName = 'Home & Living'; categoryDescription = 'Furniture and home goods'; displayOrder = 2 }
$catFash = Post '/categories' @{ categoryName = 'Fashion'; categoryDescription = 'Clothing and accessories'; displayOrder = 3 }
Write-Host ("Category IDs - electronics=$($catElec.id) home=$($catHome.id) fashion=$($catFash.id)")

# 3) Seller applies to become a seller (starts UNVERIFIED)
$profile = Post ("/users/$($seller.id)/seller-profile") @{
    shopName             = 'Demo Shop'
    shopDescription      = 'A demo shop selling sample products for the ecommerce showcase.'
    bankAccountEncrypted = 'demo-bank-account'
}
$sellerId = $profile.sellerId
Write-Host ("Seller profile ID=$sellerId status=$($profile.verificationStatus)")

# 4) Admin verifies the seller
$verified = Post ("/seller-profiles/$sellerId/verify?adminUserId=$($admin.id)") @{}
Write-Host ("Seller verificationStatus=$($verified.verificationStatus)")

# 5) Products
$products = @(
    @{ n = 'Wireless Bluetooth Headphones'; d = 'Over-ear noise-cancelling wireless headphones'; sku = 'ELEC-HP-001'; price = 89.99; stock = 25; cat = $catElec.id; disc = 15.0 },
    @{ n = '4K Smart TV 55 inch';            d = 'Ultra HD smart television with built-in streaming';  sku = 'ELEC-TV-002'; price = 499.00; stock = 10; cat = $catElec.id; disc = 5.0 },
    @{ n = 'Stainless Steel Cookware Set';   d = '12-piece premium stainless steel cookware set';       sku = 'HOME-CW-003'; price = 129.99; stock = 40; cat = $catHome.id; disc = 0.0 },
    @{ n = 'Memory Foam Pillow';             d = 'Ergonomic cooling memory foam pillow';                 sku = 'HOME-PL-004'; price = 39.99; stock = 60; cat = $catHome.id; disc = 10.0 },
    @{ n = 'Classic Denim Jacket';           d = 'Vintage-style classic denim jacket';                   sku = 'FASH-DJ-005'; price = 59.99; stock = 30; cat = $catFash.id; disc = 20.0 },
    @{ n = 'Running Sneakers';               d = 'Lightweight cushioned running sneakers';               sku = 'FASH-SN-006'; price = 74.50; stock = 35; cat = $catFash.id; disc = 0.0 }
)
foreach ($p in $products) {
    $created = Post ("/sellers/$sellerId/products") @{
        productName        = $p.n
        productDescription = $p.d
        sku                = $p.sku
        price              = $p.price
        stockQuantity      = $p.stock
        categoryId         = $p.cat
        discountPercentage = $p.disc
    }
    Write-Host ("  + product id=$($created.id) '$($created.productName)' status=$($created.productStatus)")
}

Write-Host 'Done. Demo logins (all passwords: 12345678):'
Write-Host '  Buyer : buyer@demo.com'
Write-Host '  Seller: seller@demo.com'
Write-Host '  Admin : admin@demo.com'
Write-Host '  Admin : kemheng.cheng@chipmong.com'
