import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { VerifyComponent } from './verify';

describe('VerifyComponent', () => {
  let component: VerifyComponent;
  let fixture: ComponentFixture<VerifyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyComponent],
      providers: [provideHttpClient(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and display verification challenge details', () => {
    expect(component).toBeTruthy();
    expect(component.verification).toBeTruthy();
    expect(component.verification.verificationRequestId).toBe('VR-89102-X');
    expect(component.actionCompleted).toBe(false);
  });
});
