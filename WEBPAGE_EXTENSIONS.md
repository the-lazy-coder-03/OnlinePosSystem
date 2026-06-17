# Webpage Extension Names

This file lists the main webpage names and browser paths used by the application.

## Public Pages

| Page name | Browser path / extension | Template file | Notes |
| --- | --- | --- | --- |
| Home | `/` | `index.html` | Main home page |
| Home | `/home` | `index.html` | Home page alias |
| Menu | `/menu` | `index.html` | Menu section on the home page |
| Kenridge Menu | `/menu/kenridge` | `index.html` | Kenridge branch menu |
| Uitzicht Menu | `/menu/uitzicht` | `index.html` | Uitzicht branch menu |
| Place Order | `/order` | `PlaceOrder.html` | Customer order page |
| Login | `/login` | `login.html` | User login page |
| Register | `/register` | `register.html` | User registration page |
| Forgot Password | `/forgot-password` | `forgot-password.html` | Password reset request page |
| Reset Password | `/reset-password?token=...` | `reset-password.html` | Password reset form |
| Error | `/error` | `error.html` | Application error page |

## Logged-In User Pages

| Page name | Browser path / extension | Template file | Notes |
| --- | --- | --- | --- |
| Edit Profile | `/profile/edit` | `customerInfoEdit.html` | Customer profile edit page |

## Staff And Admin Pages

| Page name | Browser path / extension | Template file | Notes |
| --- | --- | --- | --- |
| Staff Orders | `/input-orders` | `InputOrders.html` | Staff order queue page |
| Staff Orders | `/orders` | `InputOrders.html` | Staff order queue alias |
| Staff Orders | `/InputOrders` | `InputOrders.html` | Staff order queue alias |
| Staff Orders | `/InputOrders.html` | `InputOrders.html` | Staff order queue alias |
| Admin Dashboard | `/admin` | `admin.html` | Admin menu and pricing dashboard |
| Admin Live Orders | `/admin/orders` | `InputOrders.html` | Admin live orders page |

## Template Files Without A Confirmed Page Route

| File name | Notes |
| --- | --- |
| `test.html` | Template file exists, but no matching controller route was found |
| `sql-code` | Resource file exists in the templates folder |
| `prompt` | Resource file exists in the templates folder |

## API Routes

These are backend routes, not normal webpages:

| API area | Path |
| --- | --- |
| Orders API | `/api/orders` |
| Admin Orders API | `/api/admin/orders` |
| Admin Order Stream | `/api/admin/orders/stream` |
| Admin Order Webhook | `/api/admin/orders/webhook` |
| Authentication API | `/api/auth/login` |
| Staff API | `/api/staff` |
| Address API | `/api/full-address` |
