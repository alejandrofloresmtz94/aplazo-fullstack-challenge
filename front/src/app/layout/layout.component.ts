import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink, RouterOutlet, NavigationEnd } from '@angular/router';
import { AplazoButtonComponent } from '@apz/shared-ui/button';
import { AplazoDashboardComponents } from '@apz/shared-ui/dashboard';
import { AplazoSidenavLinkComponent } from '@apz/shared-ui/sidenav';
import { ROUTE_CONFIG } from '../config/routes.config';
import { filter, map } from 'rxjs/operators';
import { LoginService } from '../services/login.service';

@Component({
  standalone: true,
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  imports: [
    AplazoDashboardComponents,
    AplazoButtonComponent,
    AplazoSidenavLinkComponent,
    RouterOutlet,
    RouterLink,
  ],
})
export class LayoutComponent implements OnInit {
  readonly #router = inject(Router);
  readonly #loginService = inject(LoginService);

  readonly appRoutes = ROUTE_CONFIG;
  pageTitle = 'Layout Principal';
  
  private readonly routeTitles: Record<string, string> = {
    [ROUTE_CONFIG.home]: 'Inicio - Crear Préstamo',
    [ROUTE_CONFIG.historial]: 'Historial de Préstamos'
  };

  ngOnInit(): void {
    // Establecer título inicial
    this.updateTitle();
    
    // Escuchar cambios de ruta
    this.#router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => this.#router.url)
    ).subscribe(() => {
      this.updateTitle();
    });
  }
  
  private updateTitle(): void {
    const currentUrl = this.#router.url;
    
    // Obtener el último segmento de la URL
    const urlSegments = currentUrl.split('/');
    const lastSegment = urlSegments[urlSegments.length - 1];
    
    // Establecer el título basado en la ruta
    this.pageTitle = this.routeTitles[lastSegment] || 'Layout Principal';
  }

  clickLogo(): void {
    this.#router.navigate([`/${ROUTE_CONFIG.app}`, ROUTE_CONFIG.home]);
  }
  
  logout(): void {
    // Usar el servicio de login para cerrar sesión
    this.#loginService.logout();
  }
}
