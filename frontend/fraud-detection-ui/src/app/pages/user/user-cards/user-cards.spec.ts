import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { UserCards } from './user-cards';

describe('UserCards', () => {
  let component: UserCards;
  let fixture: ComponentFixture<UserCards>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserCards],
      providers: [provideHttpClient(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(UserCards);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and populate cards', () => {
    expect(component).toBeTruthy();
    expect(component.cards.length).toBeGreaterThan(0);
    expect(component.selectedCard).toBeTruthy();
  });

  it('should select a card and update daily limit', () => {
    if (component.cards.length > 0) {
      component.selectCard(component.cards[0]);
      expect(component.selectedCard?.id).toBe(component.cards[0].id);

      component.saveLimit();
      expect(component.statusMessage).toContain('updated');
    }
  });
});
